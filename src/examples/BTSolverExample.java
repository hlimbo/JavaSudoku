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
		SudokuFile SudokuFileFromFile = SudokuBoardReader.readFile("ExampleSudokuFiles/PH5.txt");
		BTSolver solver = new BTSolver(SudokuFileFromFile);
		
		
		
<<<<<<< HEAD
		solver.setConsistencyChecks(ConsistencyCheck.None);
		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.None);
		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.Degree);
		solver.setNakedConsistency(NakedCheck.None);
=======
		solver.setConsistencyChecks(ConsistencyCheck.ForwardChecking);
		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.LeastConstrainingValue);
		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MinimumRemainingValue);
		solver.setNakedConsistency(NakedCheck.NakedTriples);
>>>>>>> 621f5af2769065f378927506a04346c254d67ebc
		
		Thread t1 = new Thread(solver);
		try
		{
			t1.start();
<<<<<<< HEAD
			t1.join(300000);//t1.join(60000);
=======
			t1.join(1800000);
>>>>>>> 621f5af2769065f378927506a04346c254d67ebc
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
