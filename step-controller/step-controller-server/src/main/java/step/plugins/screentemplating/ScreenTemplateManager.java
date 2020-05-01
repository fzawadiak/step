package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.commons.activation.Activator;
import step.core.objectenricher.ObjectPredicate;

public class ScreenTemplateManager {

	protected final List<ScreenTemplateChangeListener> listeners = new ArrayList<>();
	
	protected ScreenInputAccessor screenInputAccessor;

	public ScreenTemplateManager(ScreenInputAccessor screenInputAccessor) {
		super();
		this.screenInputAccessor = screenInputAccessor;
	}

	public List<Input> getInputsForScreen(String screenId, Map<String,Object> contextBindings, ObjectPredicate objectPredicate) {
		Stream<ScreenInput> stream = screenInputAccessor.getScreenInputsByScreenId(screenId).stream();
		if(objectPredicate != null) {
			stream = stream.filter(objectPredicate);
		}
		List<Input> screenInputs = stream.map(i->i.getInput()).collect(Collectors.toList());
		
		List<Input> result = new ArrayList<>();
		List<Input> inputs =  Activator.findAllMatches(contextBindings, screenInputs);
		for(Input input:inputs) {
			List<Option> options = input.getOptions();
			List<Option> activeOptions = null;
			if(options!=null) {
				activeOptions = Activator.findAllMatches(contextBindings, options);
			}
			Input clone = new Input(input.getType(), input.getId(), input.getLabel(), input.getDescription(), activeOptions);
			clone.setValueHtmlTemplate(input.getValueHtmlTemplate());
			clone.setSearchMapperService(input.getSearchMapperService());
			result.add(clone);
		}
		
		return result;
	}
	
	public void moveInput(String inputId, int offset) {
		ScreenInput screenInput = screenInputAccessor.get(new ObjectId(inputId));
		
		List<ScreenInput> screenInputs = screenInputAccessor.getScreenInputsByScreenId(screenInput.getScreenId());
		
		int indexOf = screenInputs.indexOf(screenInput);
		int newIndex = indexOf+offset;
		
		if(newIndex>=0 && newIndex<screenInputs.size()) {
			Collections.swap(screenInputs, indexOf, indexOf+offset);
			for(int i=0;i<screenInputs.size();i++) {
				ScreenInput input = screenInputs.get(i);
				input.setPosition(i);
				screenInputAccessor.save(input);
			}
		}
	}
	
	public void registerListener(ScreenTemplateChangeListener listener) {
		listeners.add(listener);
	}
	
	public void notifyChange() {
		listeners.forEach(l->l.onChange());
	}

}
