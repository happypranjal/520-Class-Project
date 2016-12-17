/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.SceneTransformer;

import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.Expr;
import soot.jimple.Stmt;
import soot.jimple.internal.ConditionExprBox;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class RunnerClass {

	public static void main(String[] args) {
		SootClass sootClass = Scene.v().loadClassAndSupport("TestLoop");
		sootClass.setApplicationClass();

		Body body = null;
		for (SootMethod method : sootClass.getMethods()) {
			if (method.getName().equals("foo")) {
				if (method.isConcrete()) {
					body = method.retrieveActiveBody();
					break;
				}
			}
		}

		System.out.println("**** Jimple Code ****");
		System.out.println(body);
		System.out.println();



		UnitGraph graph = new BriefUnitGraph(body);
		Analysis analysis = new Analysis(graph);

		// use G.v().out instead of System.out so that Soot can
		// redirect this output to the Eclipse console
		G.v().out.println(body.getMethod());

		for(Unit unit : graph){ //Get all the units
			boolean isLoop = false;
			FlowSet fsb = (FlowSet) analysis.getFlowBefore(unit);
			FlowSet fsa = (FlowSet) analysis.getFlowAfter(unit);
			FlowSet fsc = (FlowSet) analysis.getFlowAfter(unit);
			fsa.difference(fsb, fsc);
			if(!fsc.isEmpty()){ 
				//Check if flow set after is not Empty
				
				for(Object statementBox : unit.getUseAndDefBoxes()){ 
					//Iterate through unit boxes
					
					if(statementBox instanceof ConditionExprBox){ 
						//If StatementBox is an instance of Express Box
						
						G.v().out.println("Condition: " + unit); 
						//Print out the condition
						
						LoopNestTree loopNest = new LoopNestTree(body); 
						//Create a new Loop Nest Tree
						
						for(Loop loop: loopNest){ 
							//Iterate through the loop
							
							if(loop.getLoopStatements().contains(unit) && !isLoop){ 
								//Check if the condition is a loop
								
								System.out.println("This Condition is a loop");
								isLoop = true; //This is a loop
								
								//For Every variable in the statements inside the loop block check if any changed variables in the statement are there in the loop condition 
								for(Stmt stm : loop.getLoopStatements()) {
									FlowSet fsb2 = (FlowSet) analysis.getFlowBefore(stm);
									FlowSet fsa2 = (FlowSet) analysis.getFlowAfter(stm);
									FlowSet fsc2 = (FlowSet) analysis.getFlowAfter(stm);
									fsa2.difference(fsb2, fsc2);
									if(!fsc2.isEmpty() && !stm.getDefBoxes().isEmpty()){
										for(ValueBox condVarBox : stm.getDefBoxes()){
											for(Object unitVarBox : unit.getUseAndDefBoxes()){
												if(unitVarBox instanceof ImmediateBox){
													//If a variable is found that is a loop variable
													if(((ImmediateBox) unitVarBox).getValue().equals(condVarBox.getValue())){
														System.out.println("The variable " + condVarBox.getValue()+ " is a loop variable");
														System.out.println();
													}
												}
											}
										}
									}
								}
							}						
						}
						//If No loop is found check the variable in the conditional statement for global dependency
						if(!isLoop){
							for(Object unitVarBox : unit.getUseAndDefBoxes()){
								if(unitVarBox instanceof ImmediateBox){
									//Ignore constants
									if(!isNumeric(((ImmediateBox) unitVarBox).getValue().toString())){
										//If the variables start with $ according to Jimple api they are global variables else they are local 
										if(((ImmediateBox) unitVarBox).getValue().toString().charAt(0) == '$'){
											System.out.println("The variable " + ((ImmediateBox) unitVarBox).getValue() + " is a Global Variable");
											System.out.println();
										}else{
											System.out.println("The variable " + ((ImmediateBox) unitVarBox).getValue() + " is a Local Variable");
											System.out.println();
										}
									}
								}
							}
						}
					}
				}
			}
		}


	}

	/**
	 *  This method checks if the value in a string is a boolean
	 *  
	 * @param str
	 * @return boolean
	 */
	static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}


	
}