package representation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import moead.CrossoverOperator;
import moead.Individual;
import moead.MOEAD;

public class FragmentedCrossoverOperator extends CrossoverOperator {

	@Override
	public Individual doCrossover(Individual ind1, Individual ind2, MOEAD init) {
		if (!(ind1 instanceof FragmentedIndividual) || !(ind2 instanceof FragmentedIndividual))
			throw new RuntimeException("FragmentedCrossoverOperator can only work on objects of type FragmentedIndividual.");
		FragmentedIndividual t1 = ((FragmentedIndividual)ind1);
		FragmentedIndividual t2 = ((FragmentedIndividual)ind2);

		// Get all fragment roots from both candidates
		List<String> allT1Keys = new ArrayList<String>(t1.getPredecessorMap().keySet());
        List<String> allT2Keys = new ArrayList<String>(t2.getPredecessorMap().keySet());

        // Shuffle them so that the crossover is done randomly
        Collections.shuffle( allT1Keys, init.random );
        Collections.shuffle( allT2Keys, init.random );

        // Select the fragment root for crossover
        String selected = null;

        for (String s1 : allT1Keys) {
            if (!s1.equals("start")) {
                for (String s2 : allT2Keys) {
                    if (s1.equals( s2 )) {
                        selected = s1;
                        break;
                    }
                }
            }
        }

        // Create replacement fragments
        Map<String, Set<String>> t1Replacements = findRelevantReplacements(selected, t2);
        Map<String, Set<String>> t2Replacements = findRelevantReplacements(selected, t1);

        // Add replacement fragments to original candidates
        addReplacementFragments(t1, t1Replacements, selected);
        addReplacementFragments(t2, t2Replacements, selected);

		// Evaluate the two children, and return the better one if there is a dominance
		t1.evaluate();
		t2.evaluate();
		if (MOEAD.dynamicNormalisation) {
			t1.finishCalculatingFitness();
			t2.finishCalculatingFitness();
		}

		if (t1.dominates(t2))
			return t1;
		else if(t2.dominates(t1))
			return t2;
		else {
			if (init.random.nextBoolean())
				return t1;
			else
				return t2;
		}
	}

	private Map<String, Set<String>> findRelevantReplacements(String selected, FragmentedIndividual t) {
	    Map<String, Set<String>> originalMap = t.getPredecessorMap();
	    Map<String, Set<String>> replacementMap = new HashMap<String, Set<String>>();

	    Queue<String> queue = new LinkedList<String>();
	    queue.offer( selected );
	    while(!queue.isEmpty()) {
	        String current = queue.poll();
	        if (!replacementMap.containsKey( current )) {
	            Set<String> newValues = new HashSet<String>(originalMap.get(current));
	            replacementMap.put( current, newValues );
	            for (String n : newValues) {
	                queue.offer( n );
	            }
	        }
	    }
	    return replacementMap;
	}

	private void addReplacementFragments(FragmentedIndividual t, Map<String, Set<String>> replacements, String selected) {
	    Map<String, Set<String>> originalMap = t.getPredecessorMap();

	    // Add the main fragment
	    Set<String> newValues = replacements.get( selected );
	    originalMap.put( selected, newValues );

	    // Recurse down and add any additional fragments that are needed
	    Queue<String> queue = new LinkedList<String>();
	    for(String s : newValues) {
	        queue.offer(s);
	    }

	    while(!queue.isEmpty()) {
	        String current = queue.poll();
	        if (!originalMap.containsKey( current )) {
                Set<String> v = new HashSet<String>(replacements.get(current));
                originalMap.put( current, v );
                for (String n : v) {
                    queue.offer( n );
                }
            }
	    }
	}
}
