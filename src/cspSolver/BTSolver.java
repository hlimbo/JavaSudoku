package cspSolver;
import helper.MapUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sudoku.Converter;
import sudoku.SudokuFile;
import sun.awt.SunHints.Value;

/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	
	public enum VariableSelectionHeuristic 	{ None, MinimumRemainingValue, Degree };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency };
	public enum NakedCheck    { None, NakedPairs, NakedTriples };
	
	private VariableSelectionHeuristic varHeuristics;
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;
	 private NakedCheck nCheck;
	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	

    public void setNakedConsistency(NakedCheck nck)
    {
            this.nCheck = nck;
    }
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}
	
	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkNakedConsistency()
	{
		boolean isConsistent = false;
		switch(nCheck)
		{
			case None: 				isConsistent = true;
			break;
	        case NakedPairs:    isConsistent = nakedPairs();
	        break;
	        case NakedTriples:    isConsistent = nakedTriples();
	        break;
			default: 				isConsistent = true;
			break;
		}
		return isConsistent;
	}
	
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement forward checking. 
	 * returns false if an unassigned variable in the network's domain is reduced to 0.
	 * otherwise return true
	 */
	private boolean forwardChecking()
	{
	   List<Variable> assignedVariables = new ArrayList<Variable>();
		
		for(Variable v : this.network.getVariables())
		{
			if(v.isAssigned())
			{
				assignedVariables.add(v);
			}
		}
		
		for(Variable assignedVariable : assignedVariables)
		{
			
			List<Variable> assignedVariableNeighbors = this.network.getNeighborsOfVariable(assignedVariable);
			for(Variable neighbor : assignedVariableNeighbors)
			{
				//variables are inconsistent and must backtrack
				if(neighbor.isAssigned() && neighbor.getAssignment() == assignedVariable.getAssignment())
				{
					return false;
				}
				
				//reduce the neighbor's domain by 1.
				if(neighbor.getDomain().contains(assignedVariable.getAssignment()))
				{
					neighbor.removeValueFromDomain(assignedVariable.getAssignment());					
				}
								
			}
		}
		
		return true;
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}
	
	//inner class for pairs
	class Pair
	{
		private Integer[] arr;
		
		public Pair()
		{
			arr = new Integer[2];
		}
		
		public Pair(Integer firstValue,Integer secondValue)
		{
			this();
			arr[0] = firstValue;
			arr[1] = secondValue;
		}
		
		public Integer getFirstValue() { return arr[0]; }
		public Integer getSecondValue() {return arr[1]; }
		
		@Override
		public String toString()
		{
			return "(" + this.getFirstValue() + ", " + this.getSecondValue() + ")";
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o == this)
				return true;
			if(!(o instanceof Pair))
				return false;
			
			Pair other = (Pair)o;
			boolean isEquivalent = ( other.getFirstValue().equals(this.getFirstValue()) && other.getSecondValue().equals(this.getSecondValue()) );
			boolean isEquivalentReversed = ( other.getFirstValue().equals(this.getSecondValue()) && other.getSecondValue().equals(this.getFirstValue()) );
			
			if(isEquivalent || isEquivalentReversed)
				return true;
			
			return false;
		}
		
	}
	
	/**
	 * TODO: Implement naked pairs. 
	 */
	private boolean nakedPairs()
	{
		List<Variable> variables = this.network.getVariables();
		List<Variable> unassigned_variables = new ArrayList<Variable>();
		
		//unassigned_variables.size() == listOfValues.size() == listOfPossiblePairs.size()
		
		for(Variable variable : variables)
		{
			if(!variable.isAssigned())
			{
				unassigned_variables.add(variable);
			}
		}
		
		//holds a list of values per unassigned variable sorted in ascending order.
		List<List<Integer>> listOfValues = new ArrayList<List<Integer>>();
		
		for(Variable variable : unassigned_variables)
		{
			listOfValues.add(this.getValuesInOrder(variable));
		}
		
		System.out.println(variables.get(0).getDomain());
		
		//holds a list of possible pairs in a set per unassigned variable.
		List<ArrayList<Pair>> listOfPossiblePairs = new ArrayList<ArrayList<Pair>>(); 
		
		//retrieve all possible combinations of pairs per unassigned_variable.
		for(int i = 0;i < listOfValues.size();++i)
		{
			ArrayList<Pair> possible_pairs = new ArrayList<Pair>();
			List<Integer> list = listOfValues.get(i);
			
			for(int j = 0;j  < list.size(); ++j)
			{
				for(int k = 0;k < list.size(); ++k)
				{
					Integer first = list.get(j);
					Integer second = list.get(k);
					
					if(first == second)
						continue;
					
					Pair candidatePair = new Pair(first,second);
										
					//if the possible pair already has a pair of equal values but different order, goto the next possible pair.. i.e. (1,7) and (7,1) are treated as the same.
					if(!possible_pairs.contains(candidatePair))
						possible_pairs.add(candidatePair);
					
				}
				
				
			}
			
			listOfPossiblePairs.add(possible_pairs);
			
			//testing code ~ used to see if pairs added in a list are equivalent regardless of order.
//			int count = 1;
//			for(Pair pair : possible_pairs)
//			{
//				System.out.println(count + ". " + pair );
//				++count;
//			}
		}
		
		//naked pairs implementation starts here!
		for(int i = 0;i < unassigned_variables.size();++i)
		{
			List<Pair> listOfPairs = listOfPossiblePairs.get(i);
			for(Pair pair : listOfPairs)
			{
				Variable unassignedVariable = unassigned_variables.get(i);
				for(Variable neighbor : this.network.getNeighborsOfVariable(unassignedVariable))
				{
					ArrayList<Integer> neighborDomainValues = neighbor.getDomain().getValues();
					if(neighborDomainValues.contains(pair.getFirstValue()) && neighborDomainValues.contains(pair.getSecondValue()))
					{
						
					}
				}
			}
		}
		
		
		
		return false;
	}
	
	/**
	 * TODO: Implement naked triples.
	 */
	private boolean nakedTriples()
	{
		return false;
	}
	
	
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{
		case None: 					next = getfirstUnassignedVariable();
		break;
		case MinimumRemainingValue: next = getMRV();
		break;
		case Degree:				next = getDegree();
		break;
		default:					next = getfirstUnassignedVariable();
		break;
		}
		return next;
	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{
		List<Variable> listOfVariables = this.network.getVariables();
		Variable minimum = null;
		for(Variable v: listOfVariables){
			if(v.isAssigned()) 
				continue;
			if(minimum == null || v.getDomain().size() < minimum.getDomain().size()){
				minimum = v;
			}
		}
		return minimum;
	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()
	{
		Variable returnValue = null;
		List<Variable> listOfVariables = this.network.getVariables();
		for(Variable v: listOfVariables){
			if(!v.isAssigned()){
				returnValue = v;
			}
		}
		
		if(returnValue == null){
			return null;
		}
		
		for(Variable v: listOfVariables){
			if(!v.isAssigned()){
				List<Variable> neighbors = this.network.getNeighborsOfVariable(v);
				int unassignedCount = 0;
				//this gives us the size of the neighbors of v which are all unassigned
				for(Variable v2: neighbors){
					if(!v2.isAssigned())
						++unassignedCount;
				}
				
				List<Variable> neighborsOfReturnValue = this.network.getNeighborsOfVariable(returnValue);
				int returnValueNeighborUnassignedCount = 0;
				for(Variable v2: neighborsOfReturnValue){
					if(!v2.isAssigned())
						++returnValueNeighborUnassignedCount;
				}
				if(unassignedCount > returnValueNeighborUnassignedCount){
					returnValue = v;
				}
			}
		}
		return returnValue;
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	/**
	 * TODO: LCV heuristic
	 * obtain integer values from variable from least constraining to most constraining
	 * Implementation: sorted by least conflicting values to most conflicting values.
	 * A conflict occurs when the value chosen for a selected variable reduces its neighbors' domain size by 1.
	 */
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		
		List<Variable> neighbors = this.network.getNeighborsOfVariable(v);
		List<Integer> values = v.getDomain().getValues();
		
		//only check unassigned neighbors.  ~ this seems to increase the number of assignments and backtracks by a constant factor.. Tested on PM1.txt
//		List<Variable> unassigned_neighbors = new ArrayList<Variable>();
//		for(Variable neighbor : neighbors)
//		{
//			if(!neighbor.isAssigned())
//			{
//				unassigned_neighbors.add(neighbor);
//			}
//		}
		
		//the sum of all conflicts that occurred per value available in the selected variable's domain.
		List<Integer> conflictSums = new ArrayList<Integer>();
		
		for(int i = 0;i < values.size(); ++i)
		{	
			Integer numConflicts = 0;
			for(Variable neighbor : neighbors)
			{
				if(neighbor.getDomain().contains(v.Values().get(i)))
				{
					++numConflicts;
				}
				
			}
			
			conflictSums.add(i, numConflicts);
		}
		
		//key: value from selected variable's domain. (number that can be put on a sudoku slot).
		//value: number of conflicts associated with selected value from variable.
		Map<Integer,Integer> valueDomainPairsMap = new HashMap<Integer,Integer>();
		for(int i = 0;i < values.size();++i)
		{
			valueDomainPairsMap.put(values.get(i), conflictSums.get(i));
		}
		
		
		//put back sorted values by total neighboring domain sizes in descending order.
		List<Integer> sortedValuesByDomainSize = new ArrayList<Integer>();
		valueDomainPairsMap = MapUtil.sortByValue(valueDomainPairsMap);
		for(Map.Entry<Integer,Integer> entry : valueDomainPairsMap.entrySet())
		{	
			//System.out.println("Value: " + entry.getKey() + ", Domain Size: " + entry.getValue());
			sortedValuesByDomainSize.add(entry.getKey());
		}
		
		return sortedValuesByDomainSize;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {
			solve(0);
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		

			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}

			//loop through the values of the variable being checked LCV

			
			for(Integer i : getNextValues(v))
			{
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;
				boolean isConsistent = checkConsistency() && checkNakedConsistency();
				
				//move to the next assignment
				if(isConsistent)
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		solve();
	}
}
