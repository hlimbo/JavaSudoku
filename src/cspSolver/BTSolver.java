package cspSolver;
import helper.MapUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
	   
	   List<Variable> allVariables = this.network.getVariables();
	   
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
				
				if(neighbor.getDomain().isEmpty())
				{
					return false;
				}
								
			}
		}
		
		return this.assignmentsCheck();
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		boolean changed = false;
		Set<Pair> arcQueue = new HashSet<Pair>();
		
		for(Variable v: this.network.getVariables()){
			for(Constraint c : this.network.getConstraintsContainingVariable(v)){
				List<Variable> varsInConstraint = c.vars;
				for(Variable v2: varsInConstraint){
					
						arcQueue.add(new Pair(v,v2));
					
				}
			}
		}
		
		
		 LinkedList<Pair> arcQ = new LinkedList<Pair>();
		    for (Pair p : arcQueue)
		      arcQ.add(p);
		    // core ac-3
		    while (!arcQ.isEmpty()) {
		      Pair p = arcQ.removeFirst();
		      if (RemoveInconsistentValues(p.getFirstValue(), p.getSecondValue()) || RemoveInconsistentValues(p.getSecondValue(),p.getFirstValue())) {
		        changed = true;
		        arcQ.addLast(p);
		      }
		    }
		    return changed;
	}
	
	private boolean RemoveInconsistentValues(Variable first, Variable second){
		List<Integer> domainL = first.getDomain().getValues();
	    List<Integer> domainR = second.getDomain().getValues();
	    
	    if (domainL.isEmpty() || domainR.isEmpty()) {
	      return false;
	    }
	    int target = domainL.get(0);
	    if (domainL.size()==1 && domainR.contains(target)){
	      first.getDomain().getValues().remove(target);
	      return true;
	    } else return false;
	}
	
	
	//inner class for pairs ~ used for nakedPairs()
	class Pair
	{
		private Variable[] arr;
		
		public Pair()
		{
			arr = new Variable[2];
		}
		
		public Pair(Variable firstVariable,Variable secondVariable)
		{
			this();
			arr[0] = firstVariable;
			arr[1] = secondVariable;
		}
		
		public void put(Variable firstVariable, Variable secondVariable)
		{
			arr[0] = firstVariable;
			arr[1] = secondVariable;
		}
		
		public Variable getFirstValue() { return arr[0]; }
		public Variable getSecondValue() {return arr[1]; }
		
		public boolean isNull()
		{
			return arr[0] == null || arr[1] == null;
		}
		
		@Override
		public String toString()
		{
			return "(" + this.getFirstValue().getName() + ", " + this.getSecondValue().getName() + ")";
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o == this)
				return true;
			if(!(o instanceof Pair))
				return false;
			
			return false;
		}
		
	}
	
	/**
	 * TODO: Implement naked pairs. 
	 */
	private boolean nakedPairs()
	{
		
		//3rd attempt
		List<Variable> variables = this.network.getVariables();
		
		//list of variables with 2 values exactly left in its domain
		List<Variable> variable2Domain = new ArrayList<Variable>();
		
		//retrieve variables with domain size of 2
		for(Variable variable : variables)
		{
			if(variable.getDomain().size() == 2)
			{
				variable2Domain.add(variable);
			}
		}
		
		//if no variable pairs were found, nakedConsistencyCheck passes
		if(variable2Domain.isEmpty() || variable2Domain.size() == 1)
			return true;
	
		//find 2 variables in the variable2Domain list that have the same exact values regardless of order.
		Pair variablePair = new Pair();
		Integer[] pairToMatch = new Integer[2];
		
		outerloop:
		for(Variable firstVariable : variable2Domain)
		{
			for(Variable neighbor : this.network.getNeighborsOfVariable(firstVariable))
			{	
				if(neighbor.getDomain().size() != 2)
					continue;
				
				Integer firstValue = neighbor.getDomain().getValues().get(0);
				Integer secondValue = neighbor.getDomain().getValues().get(1);
				if(firstVariable.getDomain().contains(firstValue) && firstVariable.getDomain().contains(secondValue))
				{
					variablePair.put(firstVariable, neighbor);
					pairToMatch[0] = firstValue;
					pairToMatch[1] = secondValue;
					break outerloop;
				}
			}
		}
		
		//there are no matching variable pairs.. don't do anything
		if(variablePair.isNull())
			return true;
		
		//identify if 2 variables are on the same box, row, or col (INCLUSIVE OR.. the 2 variables matching could be in the same row and box)
		boolean areOnSameBlock = variablePair.getFirstValue().block() == variablePair.getSecondValue().block();
		boolean areOnSameRow = variablePair.getFirstValue().row() == variablePair.getSecondValue().row();
		boolean areOnSameCol = variablePair.getFirstValue().col() == variablePair.getSecondValue().col();
		
		//by default these values are -1 if 2 variables do not share a property checked below
		int sharedBlock = -1;
		int sharedRow = -1;
		int sharedCol = -1;
		
		if(areOnSameBlock)
		{
			sharedBlock = variablePair.getFirstValue().block();
		}
		
		if(areOnSameRow)
		{
			sharedRow = variablePair.getFirstValue().row();
		}
		
		if(areOnSameCol)
		{
			sharedCol = variablePair.getFirstValue().col();
		}
		
		
		//a unit is a box, row, or col that the variable pair share in common... where we search for when removing values from the domain.
		Set<Variable> unit = new HashSet<Variable>();
		
		for(Variable candidate : this.network.getNeighborsOfVariable(variablePair.getFirstValue()))
		{
			if(candidate.isAssigned() || candidate == variablePair.getFirstValue() || candidate == variablePair.getSecondValue())
				continue;
			
			if(areOnSameBlock && sharedBlock == candidate.block())
				unit.add(candidate);
			else if(areOnSameRow && sharedRow == candidate.row())
				unit.add(candidate);
			else if(areOnSameCol && sharedCol == candidate.col())
				unit.add(candidate);
		}
		
//		for(Variable candidate : this.network.getNeighborsOfVariable(variablePair.getSecondValue()))
//		{
//			if(areOnSameBlock && sharedBlock == candidate.block())
//				unit.add(candidate);
//			else if(areOnSameRow && sharedRow == candidate.row())
//				unit.add(candidate);
//			else if(areOnSameCol && sharedCol == candidate.col())
//				unit.add(candidate);			
//		}
		
		//remove values from unit if candidate's domain contains any value in variablePair
		for(Variable candidate : unit)
		{
			if(candidate.getDomain().contains(pairToMatch[0]))
				candidate.removeValueFromDomain(pairToMatch[0]);
			
			if(candidate.getDomain().contains(pairToMatch[1]))
				candidate.removeValueFromDomain(pairToMatch[1]);
			
			if(candidate.getDomain().isEmpty())
				return false;
		}
		
		return true;
	}
			
	/**
	 * TODO: Implement naked triples.
	 * 
	 */
	
	class Triple
	{
		private Variable[] arr;
		
		public Triple()
		{
			arr = new Variable[3];
		}
		
		public Triple(Variable firstVariable,Variable secondVariable, Variable thirdVariable)
		{
			this();
			arr[0] = firstVariable;
			arr[1] = secondVariable;
			arr[2] = thirdVariable;
		}
		
		public void put(Variable firstVariable, Variable secondVariable, Variable thirdVariable)
		{
			arr[0] = firstVariable;
			arr[1] = secondVariable;
			arr[2] = thirdVariable;
		}
		
		public Variable getFirstValue() { return arr[0]; }
		public Variable getSecondValue() {return arr[1]; }
		public Variable getThirdValue() {return arr[2]; }
		
		public boolean isNull()
		{
			return arr[0] == null || arr[1] == null || arr[2] == null;
		}
		
		@Override
		public String toString()
		{
			return "(" + this.getFirstValue().getName() + ", " + this.getSecondValue().getName() + ", "+ this.getThirdValue().getName() + ")";
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o == this)
				return true;
			if(!(o instanceof Pair))
				return false;
			
			return false;
		}
		
	}
	
	
	private boolean nakedTriples()
	{
		List<Variable> tripleCandidates = new ArrayList<Variable>();
		for(Variable v : this.network.getVariables())
		{
			if(v.getDomain().size() == 3)
				tripleCandidates.add(v);
		}
		
		if(tripleCandidates.isEmpty())
			return true;
		
		List<Variable> triple = new ArrayList<Variable>();
		boolean areOnSameBox = false;
		boolean areOnSameCol = false;
		boolean areOnSameRow = false;
		
		
		//this finds a list of values with a domain size 3 or 2.
		outerloop:
		for(Variable tripleCandidate : tripleCandidates)
		{
			for(Variable neighbor : this.network.getNeighborsOfVariable(tripleCandidate))
			{
				if(neighbor.getDomain().size() == 2)
				{
					if(tripleCandidate.getDomain().contains(neighbor.Values().get(0)) && tripleCandidate.getDomain().contains(neighbor.Values().get(1)))
					{
						triple.add(neighbor);
						if(triple.size() == 2)
						{
							triple.add(tripleCandidate);
							
							Variable firstVariable = triple.get(0);
							Variable secondVariable = triple.get(1);
							Variable thirdVariable = triple.get(2);
							
							areOnSameBox = firstVariable.block() == secondVariable.block() && secondVariable.block() == thirdVariable.block();
							areOnSameCol = firstVariable.col() == secondVariable.col() && secondVariable.col() == thirdVariable.col();
							areOnSameRow = firstVariable.row() == secondVariable.row() && secondVariable.row() == thirdVariable.row();
							
							if(!areOnSameBox && !areOnSameCol && !areOnSameRow)
								triple.clear();
							else
								break outerloop;
						}
					}
				}
				
				if(neighbor.getDomain().size() == 3)
				{
					if(tripleCandidate.getDomain().contains(neighbor.Values().get(0)) && tripleCandidate.getDomain().contains(neighbor.Values().get(1)) && tripleCandidate.getDomain().contains(neighbor.Values().get(2)))
					{
						triple.add(neighbor);
						if(triple.size() == 2)
						{
							triple.add(tripleCandidate);
							
							Variable firstVariable = triple.get(0);
							Variable secondVariable = triple.get(1);
							Variable thirdVariable = triple.get(2);
							
							areOnSameBox = firstVariable.block() == secondVariable.block() && secondVariable.block() == thirdVariable.block();
							areOnSameCol = firstVariable.col() == secondVariable.col() && secondVariable.col() == thirdVariable.col();
							areOnSameRow = firstVariable.row() == secondVariable.row() && secondVariable.row() == thirdVariable.row();
							
							if(!areOnSameBox && !areOnSameCol && !areOnSameRow)
								triple.clear();
							else
								break outerloop;
						}
					}
				}
			}
			
			//if no naked triples were found in the inner loop clear the triple list to avoid having invalid values in the list.
			triple.clear();
		}
		
		//used for 2/2/2 case.
		Set<Integer> union = new HashSet<Integer>();
		//handle 2/2/2 case
		if(triple.size() < 3)
		{
			//remove previous values stored from previous search to avoid invalid naked triple from forming.
			triple.clear();
			
			//find all variables in the network with domain size = 2.
			List<Variable> doubleCandidates = new ArrayList<Variable>();
			for(Variable candidate : this.network.getVariables())
			{
				if(candidate.getDomain().size() == 2)
				{
					doubleCandidates.add(candidate);
				}
			}
			
			//search for a naked triple with 2/2/2 where the union of all 3 variables domain size sum to 3.
			outerloop:
			for(Variable doubleCandidate : doubleCandidates)
			{
				for(Variable other : doubleCandidates)
				{
					if(doubleCandidate == other)
						continue;
					
					if(this.areNeighbors(doubleCandidate, other))
					{
						Integer firstValue = doubleCandidate.Values().get(0);
						Integer secondValue = doubleCandidate.Values().get(1);

						if(other.getDomain().contains(firstValue) || other.getDomain().contains(secondValue))
						{			
							triple.add(other);
							if(triple.size() == 2)
							{
								triple.add(doubleCandidate);
								
								Variable firstVariable = triple.get(0);
								Variable secondVariable = triple.get(1);
								Variable thirdVariable = triple.get(2);
								
								areOnSameBox = firstVariable.block() == secondVariable.block() && secondVariable.block() == thirdVariable.block();
								areOnSameRow =  firstVariable.row() == secondVariable.row() && secondVariable.row() == thirdVariable.row();
								areOnSameCol =  firstVariable.col() == secondVariable.col() && secondVariable.col() == thirdVariable.col();
								
								//if potential naked triple is not found on the same unit, goto the next double candidate.
								if(!areOnSameBox && ! areOnSameRow && ! areOnSameCol)
									break;
																
								//is a valid naked triple if the 3 variable's values joined in a union has a size of 3.
								for(Variable t : triple)
									union.addAll(t.Values());
								
								if(union.size() == 3)
								{
									break outerloop;//valid naked triple is found
								}
								else
								{
									union.clear();
									break; //inner loop ~ continue the search
								}
																
							}
						}						
						else if(triple.size() == 1)//if there is one other value in the triple
						{
							Integer t_firstValue = triple.get(0).Values().get(0);
							Integer t_secondValue = triple.get(0).Values().get(1);
							
							if(other.getDomain().contains(t_firstValue) || other.getDomain().contains(t_secondValue))
							{
								triple.add(other);
								
								//the size of the list becomes 2 so we don't have to check for that here.
								triple.add(doubleCandidate);
								
								Variable firstVariable = triple.get(0);
								Variable secondVariable = triple.get(1);
								Variable thirdVariable = triple.get(2);
								
								areOnSameBox = firstVariable.block() == secondVariable.block() && secondVariable.block() == thirdVariable.block();
								areOnSameRow =  firstVariable.row() == secondVariable.row() && secondVariable.row() == thirdVariable.row();
								areOnSameCol =  firstVariable.col() == secondVariable.col() && secondVariable.col() == thirdVariable.col();
								
								//if potential naked triple is not found on the same unit, goto the next double candidate.
								if(!areOnSameBox && ! areOnSameRow && ! areOnSameCol)
									break;
																
								//is a valid naked triple if the 3 variable's values joined in a union has a size of 3.
								for(Variable t : triple)
									union.addAll(t.Values());
								
								if(union.size() == 3)
									break outerloop;//valid naked triple is found
								else
								{
									union.clear();
									break; //inner loop ~ continue the search
								}
								
							}
										
						}
					}
						
				}
				
				triple.clear();
			}
		}
		
		//if no naked triples can be found for 3/3/3, 3/3/2, 3/2/2, or 2/2/2 case , pass the consistency.
		if(triple.size() < 3)
			return true;
		
		//by default these values are -1 if 3 variables do not share a property checked below
		int sharedBlock = -1;
		int sharedRow = -1;
		int sharedCol = -1;
		
		if(areOnSameBox)
		{
			sharedBlock = triple.get(0).block();
		}
		
		if(areOnSameRow)
		{
			sharedRow = triple.get(0).row();
		}
		
		if(areOnSameCol)
		{
			sharedCol = triple.get(0).col();
		}
		
		//the last index of the list holds the triple!
		Integer[] tripletToMatch = new Integer[3];
		if(triple.get(2).size() == 2)
		{
			int i = 0;
			for(Integer value : union)
			{
				tripletToMatch[i++] = value;
			}
		}
		else
		{
			tripletToMatch[0] = triple.get(2).Values().get(0);
			tripletToMatch[1] = triple.get(2).Values().get(1);
			tripletToMatch[2] = triple.get(2).Values().get(2);
		}
		
		
		//a unit is a box, row, or col that the variable pair share in common... where we search for when removing values from the domain.
		Set<Variable> unit = new HashSet<Variable>();
		
		for(Variable candidate : this.network.getNeighborsOfVariable(triple.get(0)))
		{
			if(candidate.isAssigned() || candidate == triple.get(0) || candidate == triple.get(1) || candidate == triple.get(2))
				continue;
			
			if(areOnSameBox && sharedBlock == candidate.block())
				unit.add(candidate);
			else if(areOnSameRow && sharedRow == candidate.row())
				unit.add(candidate);
			else if(areOnSameCol && sharedCol == candidate.col())
				unit.add(candidate);
		}
		
		//remove values from unit if candidate's domain contains any value in triple
		for(Variable candidate : unit)
		{
			if(candidate.getDomain().contains(tripletToMatch[0]))
				candidate.removeValueFromDomain(tripletToMatch[0]);
			
			if(candidate.getDomain().contains(tripletToMatch[1]))
				candidate.removeValueFromDomain(tripletToMatch[1]);
			
			if(candidate.getDomain().contains(tripletToMatch[2]))
				candidate.removeValueFromDomain(tripletToMatch[2]);
			
			if(candidate.getDomain().isEmpty())
				return false;
		}
		
		
		
		
		return true;		
	}
	
	
	//@returns true iff v1 and v2 are on the same row, column, or block.
	private boolean areNeighbors(Variable v1, Variable v2)
	{
		return v1.row() == v2.row() || v1.col() == v2.col() || v1.block() == v2.block();
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
	 * ~ A Constraint is a representation of a row, col, and box on a sudoku board.
	 * ~ this.network.getConstraints() holds a list of variables that are in the same row, col, or box.
	 */
	private Variable getDegree()
	{
		
		List<Variable> unassignedVariables = new ArrayList<Variable>();
		List<Variable> variables = this.network.getVariables();
				
		for(Variable variable : variables)
		{
			if(!variable.isAssigned())
				unassignedVariables.add(variable);
		}
		
		if(unassignedVariables.isEmpty())
			return null;
		
		List<Integer>  numConflictsList = new ArrayList<Integer>();
		for(int i = 0;i < unassignedVariables.size();++i)
			numConflictsList.add(0);
	
		List<Constraint> constraints = this.network.getConstraints();
		for(int i = 0;i < unassignedVariables.size(); ++i)
		{
			Variable unassignedVariable = unassignedVariables.get(i);
			
			Integer rowConstraintIndex = unassignedVariable.row();
			Integer colConstraintIndex = unassignedVariable.col() + this.sudokuGrid.getN();
			Integer blockConstraintIndex = unassignedVariable.block() + (this.sudokuGrid.getN() * 2);
			
			Constraint rowConstraint = constraints.get(rowConstraintIndex);
			Constraint colConstraint = constraints.get(colConstraintIndex);
			Constraint blockConstraint = constraints.get(blockConstraintIndex);
			
			for(Variable var : rowConstraint.vars)
			{
				if(var == unassignedVariable)
					continue;
				
				if(!var.isAssigned())
					numConflictsList.set(i, numConflictsList.get(i) + 1);
			}
			
			for(Variable var : colConstraint.vars)
			{
				if(var == unassignedVariable)
					continue;
				
				if(!var.isAssigned())
					numConflictsList.set(i, numConflictsList.get(i) + 1);
			}
			
			for(Variable var : blockConstraint.vars)
			{
				if(var == unassignedVariable)
					continue;
				
				if(!var.isAssigned())
					numConflictsList.set(i, numConflictsList.get(i) + 1);
			}
		}
		
		Variable max = unassignedVariables.get(0);
		Integer maxConflicts = numConflictsList.get(0);
		for(int i = 1;i < unassignedVariables.size();++i)
		{
			Variable unassignedVariable = unassignedVariables.get(i);
			Integer numConflicts = numConflictsList.get(i);
			
			if(maxConflicts < numConflicts)
			{
				max = unassignedVariable;
				maxConflicts = numConflicts;
			}
		}
		
		return max;
		
//		List<Variable> unassignedVariables = new ArrayList<Variable>();
//		Variable max = null;
//		int maxConstraintSize = -1;
//		for(Variable v: this.network.getVariables()){
//			if(!v.isAssigned()){
//				unassignedVariables.add(v);
//			}
//		}
//		//we have a list of every unassigned variable
//		for(Variable v: unassignedVariables){
//				//this gives us a list of constraints the variable is constrained to we need to see how many of these values are not assigned
//				Set<Variable> s = new LinkedHashSet<>();
//				List<Constraint> test = this.network.getConstraintsContainingVariable(v);
//				for(Constraint c: test){
//					List<Variable> vars = c.vars; 
//					s.addAll(vars);
//				}
//				//all constrained variables of v are now in the set s we now need to get rid of V from it as well as any assigned values
//				s.remove(v);
//				s.removeIf(v1->v1.isAssigned());
//				
//				if(s.size() > maxConstraintSize){
//					max = v;
//					maxConstraintSize = s.size();
//				}
//		}
//		return max;
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
		
		
		//put back sorted values by total neighboring domain sizes in ascending order.
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
				boolean isConsistent = checkConsistency();
				boolean isNakedConsistent = checkNakedConsistency();
				
				//move to the next assignment
				if(isConsistent && isNakedConsistent)
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
