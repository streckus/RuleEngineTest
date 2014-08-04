/*
 * A check on the derivation results
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.io.PrintWriter;

import teo.isgci.gc.ForbiddenClass;
import teo.isgci.gc.GraphClass;

/**
 * Prints pairs of ForbiddenClasses that have no inclusion, but no witness for
 * this is found.
 */
public class RCheckForbiddenNonEdge extends RCheck {

	/** Run at the end of the deductions process */
	public void after(DeducerData d, PrintWriter w) {
		boolean err = false;
		StringBuffer sb = new StringBuffer();

		sb.append("# RCheckForbiddenNonEdge\n");

		for (GraphClass gc1 : d.getGraph().vertexSet()) {
			if (!(gc1 instanceof ForbiddenClass))
				continue;
			for (GraphClass gc2 : d.getGraph().vertexSet()) {
				if (gc2 == gc1 || !(gc2 instanceof ForbiddenClass)
						|| d.containsEdge(gc1, gc2))
					continue;

				StringBuilder s = new StringBuilder();
				boolean b = ((ForbiddenClass) gc2).notSubClassOf(
						(ForbiddenClass) gc1, s);
				if (!b) {
					err = true;
					sb.append(gc1.getID() + " -> " + gc2.getID() + " : Unconfirmed non-inclusion " + gc1
							+ " (" + gc1.getID() + ") -> " + gc2 + " (" + gc2.getID() + ")\n");
					sb.append("# " + s.toString() + "\n");
				}
			}
		}
		if (err) {
			sb.append("# end sanityCheckForbiddenNonEdge");
			String all = sb.toString();
			
			System.out.println(all);
			w.append(all);
		}
	}
}

/* EOF */

