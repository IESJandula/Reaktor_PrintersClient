package es.iesjandula.reaktor.printers_client.configurations;

import java.io.File;
import java.net.URL;

import org.springframework.stereotype.Service;

import es.iesjandula.reaktor.base.resources_handler.ResourcesHandler;
import es.iesjandula.reaktor.base.resources_handler.ResourcesHandlerFile;
import es.iesjandula.reaktor.base.resources_handler.ResourcesHandlerJar;
import es.iesjandula.reaktor.base.utils.BaseException;
import es.iesjandula.reaktor.printers_client.utils.Constants;
import jakarta.annotation.PostConstruct;

/**
 * @author Francisco Manuel Benítez Chico
 */
@Service
public class InicializacionSistema
{
	/**
	 * Este método se encarga de inicializar el sistema
	 * ya sea en el entorno de desarrollo o ejecutando JAR
	 * @throws BaseException con una excepción cargando las carpetas de resources
	 */
	@PostConstruct
	public void inicializarSistema() throws BaseException
	{
		// Esta es la carpeta con las subcarpetas y configuraciones
	    ResourcesHandler printersClientConfig = this.getResourcesHandler(Constants.PRINTERS_CLIENT_CONFIG);
	    
	    if (printersClientConfig != null)
	    {
	    	// Nombre de la carpeta destino
	    	File carpetaProyecto = new File(Constants.PRINTERS_CLIENT_CONFIG_EXEC) ;
  
	    	// Copiamos los elementos de la carpeta (origen) al destino
	    	printersClientConfig.copyToDirectory(carpetaProyecto) ;
	    }
	}
	
	/**
	 * 
	 * @param resourceFilePath con la carpeta origen que tiene las plantillas
	 * @return el manejador que crea la estructura
	 */
	private ResourcesHandler getResourcesHandler(String resourceFilePath)
	{
		ResourcesHandler outcome = null;

		URL baseDirSubfolderUrl = Thread.currentThread().getContextClassLoader().getResource(resourceFilePath);
		if (baseDirSubfolderUrl != null)
		{
			if (baseDirSubfolderUrl.getProtocol().equalsIgnoreCase("file"))
			{
				outcome = new ResourcesHandlerFile(baseDirSubfolderUrl);
			}
			else
			{
				outcome = new ResourcesHandlerJar(baseDirSubfolderUrl);
			}
		}
		
		return outcome;
	}
}
