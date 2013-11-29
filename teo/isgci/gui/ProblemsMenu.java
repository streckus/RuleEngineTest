/*
 * Menu to select a preferred problem (for colouring).
 *
 * $Id$
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.gui;

//import.java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;
import teo.isgci.problem.*;
import teo.isgci.appl.*;

public class ProblemsMenu extends JMenu implements ActionListener {
    protected Vector items;
    protected ISGCIMainFrame parent;
    protected ButtonGroup group;

    public ProblemsMenu(ISGCIMainFrame parent, String label) {
        super(label);
        this.parent = parent;
        items = new Vector();
        group = new ButtonGroup();

        addRadio("None", true);

        for (Problem p: DataSet.problems)
            if (!p.isSparse())
                addRadio(p.getName(),false);
    }

    /**
     * Add a radiobutton to this menu.
     */
    private void addRadio(String s, boolean def) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(s, def);
        item.setActionCommand(s);
        item.addActionListener(this);
        add(item);
        group.add(item);
        items.addElement(item);
        
    }

    public void actionPerformed(ActionEvent event) {
        parent.graphCanvas.setProblem(
                DataSet.getProblem(event.getActionCommand()));
    }
}

/* EOF */

