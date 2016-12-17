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
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.Expr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class MyMain {

	public static void main(String[] args) {
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {

					protected void internalTransform(Body body, String phase, Map options) {
						MyAnalysis analysis = new MyAnalysis(new BriefUnitGraph(body)); //run analysis, get graph/analysis object and store for comparison later in code
						//using a brief analysis graph because we do not care about exceptions. We simply care about the state of variables and the conditionals those
						//variables make up
						LoopNestTree loopNest = new LoopNestTree(body); //retrieve loops from the jimple
						for(Loop loop: loopNest){ //check each loop in out code, for each loop, check all the statements inside of it and how they manipulate (if they do)
							//the values of the loop head
							Stmt st = loop.getHead(); //first statement in the loop, e.g. if(i<5)
							for(Stmt stm : loop.getLoopStatements()) { //start with loop head
								FlowSet fsb = (FlowSet) analysis.getFlowBefore(stm); //flowset before unit
								FlowSet fsa = (FlowSet) analysis.getFlowAfter(stm); //flowset after unit (cast to flowset)
								FlowSet fsc = (FlowSet) analysis.getFlowAfter(stm); //initializing a flow set to store diff. contents do not matter
								fsa.difference(fsb, fsc); //diff the two flowsets, store resulting set in fsc
								if(stm instanceof ConditionExpr && st.toString().equals("nop")) { //we don't want nop statements as they are no action. We want any conditional
								//expression, e.g. 5 < 4, true || false, etc.. 
									st = stm; //store the statement value to compare after running all the units in the loop
								}
								//G.v().out.println(stm.toString());
								if(!fsc.isEmpty() && fsb.size() > fsa.size()) {
									G.v().out.println(stm.toString()); //resulting statement that splits
									Iterator it = stm.getUseAndDefBoxes().iterator(); //iterator of all the use and def boxes in the loop. Allows us
									//to check every statement individually
									while(it.hasNext()) { //loop through the statements and simply executing the units
										//G.v().out.println(it.next()); //print out the next statement in the loop for debugging
									}
									//once entire loop has run, compare the values of st variables with the new st variables
									//if the variable values have changed, then we found a loop where the variables depend on
									//computations inside the loop body, e.g. i++
								}
							}
						}
						
						// use G.v().out instead of System.out so that Soot can
						// redirect this output to the Eclipse console
						G.v().out.println(body.getMethod());
						//G.v().out.println("Hello");
					}
					
				}));
		
		soot.Main.main(args);
	}

	public static class MyAnalysis extends ForwardFlowAnalysis  {
		
		FlowSet emptySet = new ArraySparseSet();
	    Map<Unit, FlowSet> unitToGenerateSet;
	    
		public MyAnalysis(BriefUnitGraph briefUnitGraph) {
			super(briefUnitGraph); 
	        unitToGenerateSet = new HashMap<Unit, FlowSet>();
			doAnalysis(); //Soot's doAnalysis()
		}

		@Override
		protected void flowThrough(Object inValue, Object unit, Object outValue) {
			FlowSet
            in = (FlowSet) inValue,
            out = (FlowSet) outValue;
			Unit u = (Unit) unit; 
			kill(in, u, out); //kill anything that we do not want
			gen(out, u); //create new units in our set to check with kill, later
		}
		
		protected void kill(FlowSet inSet, Unit u, FlowSet outSet) {
			//standard kill for the start, the difference is when we actually kill a unit
			FlowSet kills = (FlowSet)emptySet.clone();
			Iterator defIt = u.getDefBoxes().iterator();
			while (defIt.hasNext()) {
				ValueBox defBox = (ValueBox)defIt.next();

				if (defBox.getValue() instanceof Local) {
					Iterator inIt = inSet.iterator();
					while (inIt.hasNext()) {
						Expr e = (Expr)inIt.next(); //Any kind of expression, here. =, +, ||, &&, %, etc.
						Iterator eIt = e.getUseBoxes().iterator();
						while (eIt.hasNext()) {
							ValueBox useBox = (ValueBox)eIt.next();
							if (useBox.getValue() instanceof Local &&
									useBox.getValue().equivTo(defBox.getValue())) //we don't care about local vars 
								kills.add(e);
						}
					}
				}
			}
			inSet.difference(kills, outSet); //get rid of all killed units
		}
		
		/**
		 * Performs gens by iterating over the units use-boxes.
		 * If the value of a use-box is a binopExp then we add
		 * it to the outSet.
		 * @param outSet the set flowing out of the unit
		 * @param u the unit being flown through
		 */
		protected void gen(FlowSet outSet, Unit u) {
			Iterator useIt = u.getUseBoxes().iterator();
			while (useIt.hasNext()) {
				ValueBox useBox = (ValueBox)useIt.next();
				
				if (useBox.getValue() instanceof Expr) //Any kind of expression, here. =, +, ||, &&, %, etc. add to the outset, and check with kill, later
					outSet.add(useBox.getValue());
			}
		}
	

		@Override
		protected Object newInitialFlow() {
	        return emptySet.clone();
		}

		@Override
		protected Object entryInitialFlow() {
	        return emptySet.clone();
		}

		@Override
		protected void merge(Object in1, Object in2, Object out) {
			//cast to flowsets to work with more easily
			FlowSet
            inSet1 = (FlowSet) in1,
            inSet2 = (FlowSet) in2,
            outSet = (FlowSet) out;

			inSet1.intersection(inSet2, outSet); //take the intersection of of in2 with out to in1. Only want intersection because union
			//gives	
		}

		@Override
		protected void copy(Object source, Object dest) {
			//copy flowset to the output
			FlowSet
            sourceSet = (FlowSet) source,
            destSet = (FlowSet) dest;

			sourceSet.copy(destSet);			
		}

	}

}