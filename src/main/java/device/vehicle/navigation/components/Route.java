package main.java.device.vehicle.navigation.components;

import main.java.device.vehicle.navigation.interfaces.IRoadPoint;
import main.java.device.vehicle.navigation.interfaces.IRoute;
import main.java.device.vehicle.navigation.interfaces.IRouteFragment;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Objects;

public class Route extends ArrayList<IRouteFragment> implements IRoute {

    @Serial
    private static final long serialVersionUID = 8554884433683050245L;
    protected int length = 0;
    protected String routeID = null;

    public class RouteFragment implements IRouteFragment {
        protected IRoadPoint startPoint;
        protected IRoadPoint endPoint;

        public RouteFragment(IRoadPoint startPoint, IRoadPoint endPoint) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        @Override
        public IRoadPoint getStartPoint() {
        return this.startPoint;
        }

        @Override
        public void setStartPoint(IRoadPoint startPoint) {
        this.startPoint = startPoint;
        }

        @Override
        public IRoadPoint getEndPoint() {
        return this.endPoint;
        }

        @Override
        public void setEndPoint(IRoadPoint endPoint) {
        this.endPoint = endPoint;
        }



        @Override
        public String toString() {
        return "[" + this.startPoint.toString() + "-" + this.endPoint.toString() + "]";
        }
    }

    @Override
    public String getRouteID() {
        return Objects.requireNonNullElse(this.routeID, "<no-route>");
    }

    public void setRouteID(String routeID) {
this.routeID = routeID;
}

    @Override
    public IRouteFragment getFirst() throws IndexOutOfBoundsException {
        return this.get(0);
    }

    @Override
    public IRouteFragment extractFirst() throws IndexOutOfBoundsException {
        return this.remove(0);
    }

    @Override
    public int getLenght() {
    return this.length;
    }

    @Override
    public int getRemainingDistance(IRoadPoint posActual) {
        if ( this.size() == 0 || posActual == null )
            return 0;
        int distance = Math.abs(this.get(0).getEndPoint().getPosition() - posActual.getPosition());
        for(int i=1;i<this.size();i++) {
            distance += Math.abs(this.get(i).getEndPoint().getPosition()-this.get(i).getStartPoint().getPosition());
        }
        return distance;
    }

    protected void setLength(int l) {
        this.length = l;
    }

    @Override
    public String toString() {
        if ( this.isEmpty() )
            return "<none>";

        StringBuilder rs = new StringBuilder();

        for (IRouteFragment iRouteFragment : this) rs.append(iRouteFragment.toString());

        return rs.toString();
    }

    /**
    * It adds a single RouteFragment at the end of the route
    * @param rs RoadSegment
    * @param pos_start the start position
    * @param pos_end the end position
    * @return IRoute
    */
    @Override
    public IRoute addRouteFragment(String rs, int pos_start, int pos_end) {
        this.add(new RouteFragment(new RoadPoint(rs, pos_start), new RoadPoint(rs, pos_end)));
        this.setLength(this.getLenght()+ Math.abs(pos_end-pos_start));
        this.setRouteID("Route from " + this.get(0).getStartPoint().toString() + " to " + this.get(this.size()-1).getEndPoint().toString());
        return this;
    }

    /**
    * It returns the RouteFragment at the index position
    * @param index position
    * @return IRouteFragment
    * @throws IndexOutOfBoundsException if the index is out of bounds
    */
    @Override
    public IRouteFragment getRouteFragment(int index) throws IndexOutOfBoundsException {
        return this.get(index);
    }
}
