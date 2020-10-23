package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

class MakeMove implements Move.Visitor {

    int destination1;
    int destination2;
    boolean moveType; // !!!
    List<ScotlandYard.Ticket> tickets = new ArrayList<>(); // !!! NULL

    @Override
    public Object visit(Move.SingleMove m) {
        destination1 = m.destination;
        destination2 = m.destination;
        tickets.add(m.ticket);
        moveType = false;
        return m;
    }

    @Override
    public Object visit(Move.DoubleMove m) {
        destination1 = m.destination1;
        destination2 = m.destination2;
        tickets.add(m.ticket1);
        tickets.add(m.ticket2);
        tickets.add(ScotlandYard.Ticket.DOUBLE);
        moveType = true;
        return m;
    }
}
