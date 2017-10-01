package representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import moead.Individual;
import moead.MOEAD;
import moead.MutationOperator;
import moead.Service;

public class FragmentedMutationOperator extends MutationOperator {

	@Override
	public Individual mutate(Individual ind, MOEAD init) {
		if (!(ind instanceof FragmentedIndividual))
			throw new RuntimeException("FragmentedMutationOperator can only work on objects of type FragmentedIndividual.");

		FragmentedIndividual tree = (FragmentedIndividual) ind;

		// Randomly select a node in the tree to be mutated
        List<String> keyList = new ArrayList<String>(tree.getPredecessorMap().keySet());
        String selectedKey = "start";

        while(selectedKey.equals("start")) {
        	selectedKey = keyList.get(init.random.nextInt(keyList.size()));
        }

        // Remove the old fragment
        Map<String, Set<String>> predecessorMap = tree.getPredecessorMap();
        predecessorMap.remove(selectedKey);

        // Build the new fragment(s)
        Service s;
        if (selectedKey.equals("end"))
        	s = init.endServ;
        else
        	s = init.serviceMap.get(selectedKey);
        tree.finishConstructingTree(s, init, predecessorMap);
        tree.evaluate();

		return tree;
	}
}
