package representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import moead.Individual;
import moead.LocalSearchOperator;
import moead.MOEAD;
import moead.Service;

public class FragmentedLocalSearchOperator extends LocalSearchOperator {

	@Override
	public Individual doSearch(Individual ind, MOEAD init, int problemIndex) {
		if (!(ind instanceof FragmentedIndividual))
			throw new RuntimeException("FragmentedLocalSearchOperator can only work on objects of type FragmentedIndividual.");
		FragmentedIndividual tree = (FragmentedIndividual) ind;

		// Randomly select a node in the tree to be mutated
        List<String> keyList = new ArrayList<String>(tree.getPredecessorMap().keySet());
        String selectedKey = "start";

        while(selectedKey.equals("start"))
        	selectedKey = keyList.get(init.random.nextInt(keyList.size()));

		FragmentedIndividual bestNeighbour = (FragmentedIndividual) tree.clone();
		double bestScore;
		if (MOEAD.tchebycheff)
			bestScore = init.calculateTchebycheffScore(bestNeighbour, problemIndex);
		else
			bestScore = init.calculateScore(bestNeighbour, problemIndex);


		FragmentedIndividual neighbour;

		for (int i = 0; i < MOEAD.numLocalSearchTries; i++) {
			neighbour = (FragmentedIndividual) tree.clone();

	        // Remove the old fragment
	        Map<String, Set<String>> predecessorMap = neighbour.getPredecessorMap();
	        predecessorMap.remove(selectedKey);

	        // Build the new fragment(s)
	        Service s;
	        if (selectedKey.equals("end"))
	        	s = init.endServ;
	        else
	        	s = init.serviceMap.get(selectedKey);
	        neighbour.finishConstructingTree(s, init, predecessorMap);

	    	neighbour.evaluate();
	    	double score;
			if (MOEAD.tchebycheff)
				score = init.calculateTchebycheffScore(neighbour, problemIndex);
			else
				score = init.calculateScore(neighbour, problemIndex);

	    	// If the neighbour has a better fitness score than the current best, set current best to be neighbour
	        if (score < bestScore) {
	        	bestScore = score;
	        	bestNeighbour = neighbour;
	        }
		}
		return bestNeighbour;
	}

}
