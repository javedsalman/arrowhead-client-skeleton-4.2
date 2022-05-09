package eu.arrowhead.client.skeleton.subscriber;

import java.util.Base64;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import eu.arrowhead.client.library.ArrowheadService;
import eu.arrowhead.client.library.util.ClientCommonConstants;
import eu.arrowhead.client.skeleton.subscriber.constants.SubscriberConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.InvalidParameterException;

@SpringBootApplication
@EnableConfigurationProperties(ConfigEventProperites.class)
@ComponentScan(basePackages = {CommonConstants.BASE_PACKAGE}) //TODO: add custom packages if any
public class SubscriberMain implements ApplicationRunner {

    //=================================================================================================
	// members
	
	@Value( SubscriberConstants.$PRESET_EVENT_TYPES_WD )
	private String presetEvents;
	
	@Value(ClientCommonConstants.$CLIENT_SYSTEM_NAME)
	private String clientSystemName;
	
	@Value(ClientCommonConstants.$CLIENT_SERVER_ADDRESS_WD)
	private String clientSystemAddress;
	
	@Value(ClientCommonConstants.$CLIENT_SERVER_PORT_WD)
	private int clientSystemPort;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;
	
	@Autowired
	private ArrowheadService arrowheadService;
	
	@Autowired
	private ConfigEventProperites configEventProperites;
	
	private final Logger logger = LogManager.getLogger(SubscriberApplicationInitListener.class);
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(SubscriberMain.class, args);
		
	}

	@Override
	public void run(final ApplicationArguments args) throws Exception {
		subscribeToPresetEvents();		
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	//Sample implementation of subscription when application run started
	private void subscribeToPresetEvents() {
		
		final Map<String, String> eventTypeMap = configEventProperites.getEventTypeURIMap();
		
		if(eventTypeMap == null) {			
			logger.info("No preset events to subscribe.");		
		} else {
			
			final SystemRequestDTO subscriber = new SystemRequestDTO();
			subscriber.setSystemName(clientSystemName);
			subscriber.setAddress(clientSystemAddress);
			subscriber.setPort(clientSystemPort);
			
			if (sslEnabled) {				
				subscriber.setAuthenticationInfo( Base64.getEncoder().encodeToString(arrowheadService.getMyPublicKey().getEncoded()));				
			}
			
			for (final String eventType  : eventTypeMap.keySet()) {					
				try {					
					arrowheadService.unsubscribeFromEventHandler(eventType, clientSystemName, clientSystemAddress, clientSystemPort);				
				} catch (final Exception ex) {					
					logger.debug("Could not unsubscribe from EventType: " + eventType);
				}
				
				try {					
					arrowheadService.subscribeToEventHandler(SubscriberUtilities.createSubscriptionRequestDTO( eventType, subscriber, eventTypeMap.get( eventType )));				
				} catch (final InvalidParameterException ex) {					
					if( ex.getMessage().contains("Subscription violates uniqueConstraint rules")) {						
						logger.debug("Subscription is already in DB");
					}					
				} catch (final Exception ex) {					
					logger.debug("Could not subscribe to EventType: " + eventType);
				} 
			}
		}
	}	
}
