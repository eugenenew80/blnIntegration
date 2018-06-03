package bln.integration.registry;

public interface TemplateRegistry {
	void registerTemplate(String key, String template);
	String getTemplate(String key);
}
