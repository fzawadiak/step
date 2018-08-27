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
package step.core.accessors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;



public class AbstractCRUDAccessor<T extends AbstractIdentifiableObject> extends AbstractAccessor implements CRUDAccessor<T> {
			
	protected MongoCollection collection;
	
	private Class<T> entityClass;
	
	private static JsonProvider jsonProvider = JsonProvider.provider();
		
	public AbstractCRUDAccessor(MongoClientSession clientSession, String collectionName, Class<T> entityClass) {
		super(clientSession);
		this.entityClass = entityClass;
		collection = getJongoCollection(collectionName);
	}
	
	@Override
	public T get(ObjectId id) {
		T entity = collection.findOne(id).as(entityClass);
		return entity;
	}

	public T get(String id) {
		return get(new ObjectId(id));
	}
	
	@Override
	public T findByAttributes(Map<String, String> attributes) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		for(String key:attributes.keySet()) {
			builder.add("attributes."+key, attributes.get(key));
		}

		String query = builder.build().toString();
		return collection.findOne(query).as(entityClass);
	}
	
	@Override
	public Iterator<T> getAll() {
		return collection.find().as(entityClass);
	}
	
	@Override
	public void remove(ObjectId id) {
		collection.remove(id);
	}
	
	@Override
	public T save(T entity) {
		collection.save(entity);
		return entity;
	}
	
	@Override
	public void save(List<? extends T> entities) {
		this.collection.insert(entities.toArray());
	}
}
