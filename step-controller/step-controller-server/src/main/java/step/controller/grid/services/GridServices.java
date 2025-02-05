/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.controller.grid.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
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
import step.grid.GridImpl;
import step.grid.GridReportBuilder;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperState;
import step.grid.reports.TokenGroupCapacity;

@Path("/grid")
public class GridServices extends AbstractServices {

	protected GridImpl grid;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		grid = getContext().get(GridImpl.class);
	}
	
	private GridReportBuilder getReportBuilder() {
		return new GridReportBuilder(grid);
	}
	

	@GET
	@Path("/agent")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<AgentListEntry> getAgents() {
		List<AgentListEntry> agents = new ArrayList<>();
		
		grid.getAgents().forEach(agent->{
			AgentListEntry agentState = new AgentListEntry();
			agentState.setAgentRef(agent);
			agentState.setTokens(getAgentTokens(agent.getAgentId()));
			agentState.setTokensCapacity(getTokensCapacity(agentState.getTokens()));
			
			agents.add(agentState);
		});
		return agents;
	}

	protected List<TokenWrapper> getAgentTokens(String agentId) {
		List<TokenWrapper> agentTokens = new ArrayList<>();
		
		grid.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				agentTokens.add(token);
			}
		});
		return agentTokens;
	}
	
	
	protected TokenGroupCapacity getTokensCapacity(List<TokenWrapper> tokens) {
		TokenGroupCapacity tokenGroup = new TokenGroupCapacity(new HashMap<>());
		Map<TokenWrapperState, AtomicInteger> stateDistribution = new HashMap<>();
		Arrays.asList(TokenWrapperState.values()).forEach(s->stateDistribution.put(s, new AtomicInteger(0)));
		tokens.forEach(token->{
			tokenGroup.incrementCapacity();
			stateDistribution.get(token.getState()).incrementAndGet();
		});
		
		Map<TokenWrapperState, Integer> result = new HashMap<>();
		stateDistribution.entrySet().forEach(e->result.put(e.getKey(),e.getValue().get()));
		
		tokenGroup.setCountByState(result);
		return tokenGroup;
	}
	
	@PUT
	@Secured(right="token-manage")
	@Path("/agent/{id}/interrupt")
	@Produces(MediaType.APPLICATION_JSON)
	public void interruptAgent(@PathParam("id") String agentId) {
		grid.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				grid.startTokenMaintenance(token.getID());
			}
		});
	}
	
	@PUT
	@Secured(right="token-manage")
	@Path("/agent/{id}/resume")
	@Produces(MediaType.APPLICATION_JSON)
	public void resumeAgent(@PathParam("id") String agentId) {
		grid.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				grid.stopTokenMaintenance(token.getID());
			}
		});
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/agent/{id}/tokens/errors")
	@Produces(MediaType.APPLICATION_JSON)
	public void removeAgentTokenErrors(@PathParam("id") String agentId) {
		grid.getTokens().forEach(token->{
			if(token.getState().equals(TokenWrapperState.ERROR)) {
				grid.removeTokenError(token.getID());
			}
		});
	}

	@GET
	@Path("/token")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenWrapper> getTokenAssociations() {
		return getReportBuilder().getTokenAssociations(false);
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/token/{id}/error")
	@Consumes(MediaType.APPLICATION_JSON)
	public void removeTokenError(@PathParam("id") String tokenId) {
		grid.removeTokenError(tokenId);
	}
	
	@POST
	@Secured(right="token-manage")
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void startTokenMaintenance(@PathParam("id") String tokenId) {
		grid.startTokenMaintenance(tokenId);
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void stopTokenMaintenance(@PathParam("id") String tokenId) {
		grid.stopTokenMaintenance(tokenId);
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
