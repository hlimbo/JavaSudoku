package examples;

import cspSolver.BTSolver;
import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;
import cspSolver.BTSolver.NakedCheck;
import sudoku.SudokuBoardGenerator;
import sudoku.SudokuBoardReader;
import sudoku.SudokuFile;

public class BTSolverExample {

	public static void main(String[] args)
	{
		//SudokuFile sf = SudokuBoardGenerator.generateBoard(9, 3, 3, 12);
		SudokuFile SudokuFileFromFile = SudokuBoardReader.readFile("ExampleSudokuFiles/PH3.txt");
		BTSolver solver = new BTSolver(SudokuFileFromFile);
		
		
		
		solver.setConsistencyChecks(ConsistencyCheck.ForwardChecking);
		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.LeastConstrainingValue);
		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MinimumRemainingValue);
		solver.setNakedConsistency(NakedCheck.NakedTriples);
		
		Thread t1 = new Thread(solver);
		try
		{
			t1.start();
			t1.join(60000);
			if(t1.isAlive())
			{
				t1.interrupt();
			}
		}catch(InterruptedException e)
		{
		}


		if(solver.hasSolution())
		{
			solver.printSolverStats();
			System.out.println(solver.getSolution());	
		}

		else
		{
			System.out.println("Failed to find a solution");
		}

	}
}
