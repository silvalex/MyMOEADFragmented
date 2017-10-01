package representation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import moead.Individual;
import moead.MOEAD;
import moead.Service;

public class FragmentedIndividual extends Individual {
	private double availability;
	private double reliability;
	private double time;
	private double cost;
	private Map<String, Set<String>> predecessorMap;
	private double[] objectives;
	private MOEAD init;

	@Override
	public Individual generateIndividual() {
	    Map<String, Set<String>> pMap = new HashMap<String, Set<String>>();
	    pMap.put("start", new HashSet<String>());

	    finishConstructingTree(init.endServ, init, pMap);

	    FragmentedIndividual newInd = new FragmentedIndividual();
	    newInd.predecessorMap = pMap;
	    newInd.setInit(init);
	    newInd.evaluate();
	    return newInd;
	}

	public void finishConstructingTree(Service s, MOEAD init, Map<String, Set<String>> predecessorMap) {
	    Queue<Service> queue = new LinkedList<Service>();
	    queue.offer(s);

	    while (!queue.isEmpty()) {
	    	Service current = queue.poll();

	    	if (!predecessorMap.containsKey(current.name)) {
		    	Set<Service> predecessors = findPredecessors(init, current.getInputs(), current.layer);
		    	Set<String> predecessorNames = new HashSet<String>();

		    	for (Service p : predecessors) {
		    		predecessorNames.add(p.name);
	    			queue.offer(p);
		    	}
		    	predecessorMap.put(current.name, predecessorNames);
	    	}
	    }
	}

	public Set<Service> findPredecessors(MOEAD init, Set<String> inputs, int layer) {
		Set<Service> predecessors = new HashSet<Service>();

		// Get only inputs that are not subsumed by the given composition inputs

		Set<String> inputsNotSatisfied = init.getInputsNotSubsumed(inputs, init.startServ.outputs);
		Set<String> inputsToSatisfy = new HashSet<String>(inputsNotSatisfied);

		if (inputsToSatisfy.size() < inputs.size())
			predecessors.add(init.startServ);

		// Find services to satisfy all inputs
		for (String i : inputsNotSatisfied) {
			if (inputsToSatisfy.contains(i)) {
				List<Service> candidates = init.taxonomyMap.get(i).servicesWithOutput;
				Collections.shuffle(candidates, init.random);

				Service chosen = null;
				candLoop:
				for(Service cand : candidates) {
					if (init.relevant.contains(cand) && cand.layer < layer) {
						predecessors.add(cand);
						chosen = cand;
						break candLoop;
					}
				}

				inputsToSatisfy.remove(i);

				// Check if other outputs can also be fulfilled by the chosen candidate, and remove them also
				Set<String> subsumed = init.getInputsSubsumed(inputsToSatisfy, chosen.outputs);
				inputsToSatisfy.removeAll(subsumed);
			}
		}
		return predecessors;
	}

	@Override
	public Individual clone() {
		FragmentedIndividual newInd = new FragmentedIndividual();
		Map<String, Set<String>> pMap = new HashMap<String, Set<String>>();

		for (Entry<String, Set<String>> e : predecessorMap.entrySet()) {
			pMap.put(e.getKey(), e.getValue());
		}
		newInd.predecessorMap = pMap;

		newInd.availability = availability;
		newInd.reliability = reliability;
		newInd.time = time;
		newInd.cost = cost;
		newInd.init = init;
		newInd.objectives = new double[objectives.length];

		System.arraycopy(objectives, 0, newInd.objectives, 0, objectives.length);

		return newInd;
	}

	@Override
	public double[] getObjectiveValues() {
		return objectives;
	}

	@Override
	public void setObjectiveValues(double[] newObjectives) {
		objectives = newObjectives;
	}

	@Override
	public double getAvailability() {
		return availability;
	}

	@Override
	public double getReliability() {
		return reliability;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public double getCost() {
		return cost;
	}

	public Map<String, Set<String>> getPredecessorMap() {
		return predecessorMap;
	}

	public void createNewPredecessorMap() {
		predecessorMap = new HashMap<String, Set<String>>();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, Set<String>> e : predecessorMap.entrySet()) {
			builder.append("(");
			builder.append(e.getKey());
			builder.append(",");
			builder.append(e.getValue());
			builder.append("), ");
		}
		return builder.toString();
	}

	@Override
	public void evaluate() {
		calculateSequenceFitness(init.numLayers, init.endServ);
	}

   public void calculateSequenceFitness(int numLayers, Service end) {
        cost = 0.0;
        availability = 1.0;
        reliability = 1.0;

        Map<String, Double> timeMap = new HashMap<String, Double>();
        time = findLongestTime("end", init, timeMap);

		for (String key : timeMap.keySet()) {
		    if (!key.equals( "start" ) && !key.equals( "end" )) {
			    double[] qos = init.serviceMap.get( key ).getQos();
			    cost += qos[MOEAD.COST];
			    availability *= qos[MOEAD.AVAILABILITY];
			    reliability *= qos[MOEAD.RELIABILITY];
		    }
		}

		// Find the unused fragments from the tree
		Set<String> fragmentsToRemove = new HashSet<String>();
		for (String s : predecessorMap.keySet()) {
		    if (!timeMap.containsKey(s))
		        fragmentsToRemove.add(s);
		}
		// Clean up the unused fragments
		for (String s : fragmentsToRemove)
		    predecessorMap.remove( s );

        if (!MOEAD.dynamicNormalisation)
        	finishCalculatingFitness();
    }

	private double findLongestTime(String select, MOEAD init, Map<String, Double> timeMap) {
	    if (!timeMap.containsKey( select )) {
	        double highestTime = 0.0;

	        for(String child: predecessorMap.get( select )) {
	            double childValue;
	            if (timeMap.containsKey( child ))
	                childValue = timeMap.get( child );
	            else
	               childValue = findLongestTime(child, init, timeMap);
                if (childValue > highestTime)
                    highestTime = childValue;
	        }

	        double serviceTime = 0.0;
	        if (!select.equals("start") && !select.equals("end"))
	            serviceTime = init.serviceMap.get(select).getQos()[MOEAD.TIME];
	        double overallTime = highestTime + serviceTime;
	        timeMap.put( select, overallTime );
	        return overallTime;
	    }
	    else {
	        return timeMap.get( select );
	    }
	}

	@Override
	public void finishCalculatingFitness() {
		objectives = calculateFitness(cost, time, availability, reliability);
	}

	public double[] calculateFitness(double c, double t, double a, double r) {
        a = normaliseAvailability(a, init);
        r = normaliseReliability(r, init);
        t = normaliseTime(t, init);
        c = normaliseCost(c, init);

        double[] objectives = new double[2];
        objectives[0] = t + c;
        objectives[1] = a + r;

        return objectives;
	}

	private double normaliseAvailability(double availability, MOEAD init) {
		if (init.maxAvailability - init.minAvailability == 0.0)
			return 1.0;
		else
			return (init.maxAvailability - availability)/(init.maxAvailability - init.minAvailability);
	}

	private double normaliseReliability(double reliability, MOEAD init) {
		if (init.maxReliability- init.minReliability == 0.0)
			return 1.0;
		else
			return (init.maxReliability - reliability)/(init.maxReliability - init.minReliability);
	}

	private double normaliseTime(double time, MOEAD init) {
		if (init.maxTime - init.minTime == 0.0)
			return 1.0;
		else
			return (time - init.minTime)/(init.maxTime - init.minTime);
	}

	private double normaliseCost(double cost, MOEAD init) {
		if (init.maxCost - init.minCost == 0.0)
			return 1.0;
		else
			return (cost - init.minCost)/(init.maxCost - init.minCost);
	}

	@Override
	public void setInit(MOEAD init) {
		this.init = init;
	}

}
