import java.util.Iterator;

import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Expr;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;


public class Analysis extends ForwardFlowAnalysis  {

		FlowSet emptySet = new ArraySparseSet();

		public Analysis(UnitGraph graph) {
			super(graph);
			doAnalysis();
		}

		@Override
		protected void flowThrough(Object inValue, Object unit, Object outValue) {
			FlowSet
			in = (FlowSet) inValue,
			out = (FlowSet) outValue;
			Unit u = (Unit) unit; 
			kill(in, u, out);
			gen(out, u);
		}

		protected void kill(FlowSet inSet, Unit u, FlowSet outSet) {
			FlowSet kills = (FlowSet)emptySet.clone();
			Iterator defIt = u.getDefBoxes().iterator();
			while (defIt.hasNext()) {
				ValueBox defBox = (ValueBox)defIt.next();

				if (defBox.getValue() instanceof Local) {
					Iterator inIt = inSet.iterator();
					while (inIt.hasNext()) {
						Expr e = (Expr)inIt.next();
						Iterator eIt = e.getUseBoxes().iterator();
						while (eIt.hasNext()) {
							ValueBox useBox = (ValueBox)eIt.next();
							if (useBox.getValue() instanceof Local &&
									useBox.getValue().equivTo(defBox.getValue()))
								kills.add(e);
						}
					}
				}
			}
			inSet.difference(kills, outSet);
		}

		/**
		 * Performs gens by iterating over the units use-boxes.
		 * If the value of a use-box is a Exp then we add
		 * it to the outSet.
		 * @param outSet the set flowing out of the unit
		 * @param u the unit being flown through
		 */
		protected void gen(FlowSet outSet, Unit u) {
			Iterator useIt = u.getUseBoxes().iterator();
			while (useIt.hasNext()) {
				ValueBox useBox = (ValueBox)useIt.next();

				if (useBox.getValue() instanceof Expr)
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
			FlowSet
			inSet1 = (FlowSet) in1,
			inSet2 = (FlowSet) in2,
			outSet = (FlowSet) out;

			inSet1.intersection(inSet2, outSet);			
		}

		@Override
		protected void copy(Object source, Object dest) {
			FlowSet
			sourceSet = (FlowSet) source,
			destSet = (FlowSet) dest;

			sourceSet.copy(destSet);			
		}

	}