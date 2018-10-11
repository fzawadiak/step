/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.plugins.adaptergrid;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.grid.AgentRef;
import step.grid.Grid;
import step.grid.GridReportBuilder;
import step.grid.TokenWrapper;
import step.grid.reports.TokenGroupCapacity;

@Path("/grid")
public class GridServices extends AbstractServices {

	private Grid getAdapterGrid() {
		return getContext().get(Grid.class);
	}
	
	private GridReportBuilder getReportBuilder() {
		return new GridReportBuilder(getAdapterGrid());
	}
	

	@GET
	@Path("/agent")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<AgentRef> getAgents() {
		return getAdapterGrid().getAgents();
	}
	
	@PUT
	@Path("/agent/{id}/interrupt")
	@Produces(MediaType.APPLICATION_JSON)
	public void interruptAgent(@PathParam("id") String agentId) {
		
	}
	
	@PUT
	@Path("/agent/{id}/resume")
	@Produces(MediaType.APPLICATION_JSON)
	public void resumeAgent(@PathParam("id") String agentId) {
		
	}

	@GET
	@Path("/token")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenWrapper> getTokenAssociations() {
		return getReportBuilder().getTokenAssociations(false);
	}
	
	@DELETE
	@Secured
	@Path("/token/{id}/error")
	@Consumes(MediaType.APPLICATION_JSON)
	public void removeTokenError(@PathParam("id") String tokenId) {
		getAdapterGrid().removeTokenError(tokenId);
	}
	
	@POST
	@Secured
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void startTokenMaintenance(@PathParam("id") String tokenId) {
		getAdapterGrid().startTokenMaintenance(tokenId);
	}
	
	@DELETE
	@Secured
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void stopTokenMaintenance(@PathParam("id") String tokenId) {
		getAdapterGrid().stopTokenMaintenance(tokenId);
	}
	
	@GET
	@Path("/token/usage")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenGroupCapacity> getUsageByIdentity(@QueryParam("groupby") List<String> groupbys) {
		return getReportBuilder().getUsageByIdentity(groupbys);
	}
	
	@GET
	@Path("/keys")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getTokenAttributeKeys() {
		return getReportBuilder().getTokenAttributeKeys();
	}
}
