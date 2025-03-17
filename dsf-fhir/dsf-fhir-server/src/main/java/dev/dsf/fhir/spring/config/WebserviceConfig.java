package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.auth.logout.LogoutService;
import dev.dsf.common.status.webservice.StatusService;
import dev.dsf.common.ui.webservice.StaticResourcesService;
import dev.dsf.fhir.exception.DataFormatExceptionHandler;
import dev.dsf.fhir.webservice.filter.BrowserPolicyHeaderResponseFilter;
import dev.dsf.fhir.webservice.impl.ActivityDefinitionServiceImpl;
import dev.dsf.fhir.webservice.impl.BinaryServiceImpl;
import dev.dsf.fhir.webservice.impl.BundleServiceImpl;
import dev.dsf.fhir.webservice.impl.CodeSystemServiceImpl;
import dev.dsf.fhir.webservice.impl.ConformanceServiceImpl;
import dev.dsf.fhir.webservice.impl.DocumentReferenceServiceImpl;
import dev.dsf.fhir.webservice.impl.EndpointServiceImpl;
import dev.dsf.fhir.webservice.impl.GroupServiceImpl;
import dev.dsf.fhir.webservice.impl.HealthcareServiceServiceImpl;
import dev.dsf.fhir.webservice.impl.LibraryServiceImpl;
import dev.dsf.fhir.webservice.impl.LocationServiceImpl;
import dev.dsf.fhir.webservice.impl.MeasureReportServiceImpl;
import dev.dsf.fhir.webservice.impl.MeasureServiceImpl;
import dev.dsf.fhir.webservice.impl.NamingSystemServiceImpl;
import dev.dsf.fhir.webservice.impl.OrganizationAffiliationServiceImpl;
import dev.dsf.fhir.webservice.impl.OrganizationServiceImpl;
import dev.dsf.fhir.webservice.impl.PatientServiceImpl;
import dev.dsf.fhir.webservice.impl.PractitionerRoleServiceImpl;
import dev.dsf.fhir.webservice.impl.PractitionerServiceImpl;
import dev.dsf.fhir.webservice.impl.ProvenanceServiceImpl;
import dev.dsf.fhir.webservice.impl.QuestionnaireResponseServiceImpl;
import dev.dsf.fhir.webservice.impl.QuestionnaireServiceImpl;
import dev.dsf.fhir.webservice.impl.ResearchStudyServiceImpl;
import dev.dsf.fhir.webservice.impl.RootServiceImpl;
import dev.dsf.fhir.webservice.impl.StructureDefinitionServiceImpl;
import dev.dsf.fhir.webservice.impl.SubscriptionServiceImpl;
import dev.dsf.fhir.webservice.impl.TaskServiceImpl;
import dev.dsf.fhir.webservice.impl.ValueSetServiceImpl;
import dev.dsf.fhir.webservice.jaxrs.ActivityDefinitionServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.BinaryServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.BundleServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.CodeSystemServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.ConformanceServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.DocumentReferenceServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.EndpointServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.GroupServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.HealthcareServiceServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.LibraryServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.LocationServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.MeasureReportServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.MeasureServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.NamingSystemServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.OrganizationAffiliationServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.OrganizationServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.PatientServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.PractitionerRoleServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.PractitionerServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.ProvenanceServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.QuestionnaireResponseServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.QuestionnaireServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.ResearchStudyServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.RootServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.StructureDefinitionServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.SubscriptionServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.TaskServiceJaxrs;
import dev.dsf.fhir.webservice.jaxrs.ValueSetServiceJaxrs;
import dev.dsf.fhir.webservice.secure.ActivityDefinitionServiceSecure;
import dev.dsf.fhir.webservice.secure.BinaryServiceSecure;
import dev.dsf.fhir.webservice.secure.BundleServiceSecure;
import dev.dsf.fhir.webservice.secure.CodeSystemServiceSecure;
import dev.dsf.fhir.webservice.secure.ConformanceServiceSecure;
import dev.dsf.fhir.webservice.secure.DocumentReferenceServiceSecure;
import dev.dsf.fhir.webservice.secure.EndpointServiceSecure;
import dev.dsf.fhir.webservice.secure.GroupServiceSecure;
import dev.dsf.fhir.webservice.secure.HealthcareServiceServiceSecure;
import dev.dsf.fhir.webservice.secure.LibraryServiceSecure;
import dev.dsf.fhir.webservice.secure.LocationServiceSecure;
import dev.dsf.fhir.webservice.secure.MeasureReportServiceSecure;
import dev.dsf.fhir.webservice.secure.MeasureServiceSecure;
import dev.dsf.fhir.webservice.secure.NamingSystemServiceSecure;
import dev.dsf.fhir.webservice.secure.OrganizationAffiliationServiceSecure;
import dev.dsf.fhir.webservice.secure.OrganizationServiceSecure;
import dev.dsf.fhir.webservice.secure.PatientServiceSecure;
import dev.dsf.fhir.webservice.secure.PractitionerRoleServiceSecure;
import dev.dsf.fhir.webservice.secure.PractitionerServiceSecure;
import dev.dsf.fhir.webservice.secure.ProvenanceServiceSecure;
import dev.dsf.fhir.webservice.secure.QuestionnaireResponseServiceSecure;
import dev.dsf.fhir.webservice.secure.QuestionnaireServiceSecure;
import dev.dsf.fhir.webservice.secure.ResearchStudyServiceSecure;
import dev.dsf.fhir.webservice.secure.RootServiceSecure;
import dev.dsf.fhir.webservice.secure.StructureDefinitionServiceSecure;
import dev.dsf.fhir.webservice.secure.SubscriptionServiceSecure;
import dev.dsf.fhir.webservice.secure.TaskServiceSecure;
import dev.dsf.fhir.webservice.secure.ValueSetServiceSecure;
import dev.dsf.fhir.webservice.specification.ActivityDefinitionService;
import dev.dsf.fhir.webservice.specification.BinaryService;
import dev.dsf.fhir.webservice.specification.BundleService;
import dev.dsf.fhir.webservice.specification.CodeSystemService;
import dev.dsf.fhir.webservice.specification.ConformanceService;
import dev.dsf.fhir.webservice.specification.DocumentReferenceService;
import dev.dsf.fhir.webservice.specification.EndpointService;
import dev.dsf.fhir.webservice.specification.GroupService;
import dev.dsf.fhir.webservice.specification.HealthcareServiceService;
import dev.dsf.fhir.webservice.specification.LibraryService;
import dev.dsf.fhir.webservice.specification.LocationService;
import dev.dsf.fhir.webservice.specification.MeasureReportService;
import dev.dsf.fhir.webservice.specification.MeasureService;
import dev.dsf.fhir.webservice.specification.NamingSystemService;
import dev.dsf.fhir.webservice.specification.OrganizationAffiliationService;
import dev.dsf.fhir.webservice.specification.OrganizationService;
import dev.dsf.fhir.webservice.specification.PatientService;
import dev.dsf.fhir.webservice.specification.PractitionerRoleService;
import dev.dsf.fhir.webservice.specification.PractitionerService;
import dev.dsf.fhir.webservice.specification.ProvenanceService;
import dev.dsf.fhir.webservice.specification.QuestionnaireResponseService;
import dev.dsf.fhir.webservice.specification.QuestionnaireService;
import dev.dsf.fhir.webservice.specification.ResearchStudyService;
import dev.dsf.fhir.webservice.specification.RootService;
import dev.dsf.fhir.webservice.specification.StructureDefinitionService;
import dev.dsf.fhir.webservice.specification.SubscriptionService;
import dev.dsf.fhir.webservice.specification.TaskService;
import dev.dsf.fhir.webservice.specification.ValueSetService;

@Configuration
public class WebserviceConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private ValidationConfig validationConfig;

	@Autowired
	private SnapshotConfig snapshotConfig;

	@Autowired
	private EventConfig eventConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private CommandConfig commandConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Autowired
	private AuthorizationConfig authorizationConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private HistoryConfig historyConfig;

	@Bean
	public BrowserPolicyHeaderResponseFilter browserPolicyHeaderResponseFilter()
	{
		return new BrowserPolicyHeaderResponseFilter();
	}

	@Bean
	public DataFormatExceptionHandler dataFormatExceptionHandler()
	{
		return new DataFormatExceptionHandler(helperConfig.responseGenerator());
	}

	@Bean
	public ActivityDefinitionService activityDefinitionService()
	{
		return new ActivityDefinitionServiceJaxrs(activityDefinitionServiceSecure());
	}

	private ActivityDefinitionServiceSecure activityDefinitionServiceSecure()
	{
		return new ActivityDefinitionServiceSecure(activityDefinitionServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(),
				daoConfig.activityDefinitionDao(), helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.activityDefinitionAuthorizationRule(), validationConfig.resourceValidator());
	}

	private ActivityDefinitionServiceImpl activityDefinitionServiceImpl()
	{
		return new ActivityDefinitionServiceImpl(ActivityDefinitionServiceJaxrs.PATH,
				propertiesConfig.getServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				daoConfig.activityDefinitionDao(), validationConfig.resourceValidator(), eventConfig.eventManager(),
				helperConfig.exceptionHandler(), eventConfig.eventGenerator(), helperConfig.responseGenerator(),
				helperConfig.parameterConverter(), referenceConfig.referenceExtractor(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				authorizationConfig.authorizationRuleProvider(), historyConfig.historyService());
	}

	@Bean
	public BinaryService binaryService()
	{
		return new BinaryServiceJaxrs(binaryServiceSecure(), helperConfig.parameterConverter());
	}

	private BinaryServiceSecure binaryServiceSecure()
	{
		return new BinaryServiceSecure(binaryServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.binaryDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.binaryAuthorizationRule(), validationConfig.resourceValidator());
	}

	private BinaryService binaryServiceImpl()
	{
		return new BinaryServiceImpl(BinaryServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.binaryDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public BundleService bundleService()
	{
		return new BundleServiceJaxrs(bundleServiceSecure());
	}

	private BundleServiceSecure bundleServiceSecure()
	{
		return new BundleServiceSecure(bundleServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.bundleDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.bundleAuthorizationRule(), validationConfig.resourceValidator());
	}

	private BundleService bundleServiceImpl()
	{
		return new BundleServiceImpl(BundleServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.bundleDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public CodeSystemService codeSystemService()
	{
		return new CodeSystemServiceJaxrs(codeSystemServiceSecure());
	}

	private CodeSystemServiceSecure codeSystemServiceSecure()
	{
		return new CodeSystemServiceSecure(codeSystemServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.codeSystemDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.codeSystemAuthorizationRule(), validationConfig.resourceValidator());
	}

	private CodeSystemServiceImpl codeSystemServiceImpl()
	{
		return new CodeSystemServiceImpl(CodeSystemServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.codeSystemDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public DocumentReferenceService documentReferenceService()
	{
		return new DocumentReferenceServiceJaxrs(documentReferenceServiceSecure());
	}

	private DocumentReferenceServiceSecure documentReferenceServiceSecure()
	{
		return new DocumentReferenceServiceSecure(documentReferenceServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(),
				daoConfig.documentReferenceDao(), helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.documentReferenceAuthorizationRule(), validationConfig.resourceValidator());
	}

	private DocumentReferenceServiceImpl documentReferenceServiceImpl()
	{
		return new DocumentReferenceServiceImpl(DocumentReferenceServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.documentReferenceDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public EndpointService endpointService()
	{
		return new EndpointServiceJaxrs(endpointServiceSecure());
	}

	private EndpointServiceSecure endpointServiceSecure()
	{
		return new EndpointServiceSecure(endpointServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.endpointDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.endpointAuthorizationRule(), validationConfig.resourceValidator());
	}

	private EndpointServiceImpl endpointServiceImpl()
	{
		return new EndpointServiceImpl(EndpointServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.endpointDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public GroupService groupService()
	{
		return new GroupServiceJaxrs(groupServiceSecure());
	}

	private GroupServiceSecure groupServiceSecure()
	{
		return new GroupServiceSecure(groupServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.groupDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.groupAuthorizationRule(), validationConfig.resourceValidator());
	}

	private GroupServiceImpl groupServiceImpl()
	{
		return new GroupServiceImpl(GroupServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.groupDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public HealthcareServiceService healthcareServiceService()
	{
		return new HealthcareServiceServiceJaxrs(healthcareServiceServiceSecure());
	}

	private HealthcareServiceServiceSecure healthcareServiceServiceSecure()
	{
		return new HealthcareServiceServiceSecure(healthcareServiceServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(),
				daoConfig.healthcareServiceDao(), helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.healthcareServiceAuthorizationRule(), validationConfig.resourceValidator());
	}

	private HealthcareServiceServiceImpl healthcareServiceServiceImpl()
	{
		return new HealthcareServiceServiceImpl(HealthcareServiceServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.healthcareServiceDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public LibraryService libraryService()
	{
		return new LibraryServiceJaxrs(libraryServiceSecure());
	}

	private LibraryServiceSecure libraryServiceSecure()
	{
		return new LibraryServiceSecure(libraryServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.libraryDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.libraryAuthorizationRule(), validationConfig.resourceValidator());
	}

	private LibraryServiceImpl libraryServiceImpl()
	{
		return new LibraryServiceImpl(LibraryServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.libraryDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public LocationService locationService()
	{
		return new LocationServiceJaxrs(locationServiceSecure());
	}

	private LocationServiceSecure locationServiceSecure()
	{
		return new LocationServiceSecure(locationServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.locationDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.locationAuthorizationRule(), validationConfig.resourceValidator());
	}

	private LocationServiceImpl locationServiceImpl()
	{
		return new LocationServiceImpl(LocationServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.locationDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public LogoutService logoutService()
	{
		return new LogoutService();
	}

	@Bean
	public MeasureService measureService()
	{
		return new MeasureServiceJaxrs(measureServiceSecure());
	}

	private MeasureServiceSecure measureServiceSecure()
	{
		return new MeasureServiceSecure(measureServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.measureDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.measureAuthorizationRule(), validationConfig.resourceValidator());
	}

	private MeasureServiceImpl measureServiceImpl()
	{
		return new MeasureServiceImpl(MeasureServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.measureDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public MeasureReportService measureReportService()
	{
		return new MeasureReportServiceJaxrs(measureReportServiceSecure());
	}

	private MeasureReportServiceSecure measureReportServiceSecure()
	{
		return new MeasureReportServiceSecure(measureReportServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.measureReportDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.measureReportAuthorizationRule(), validationConfig.resourceValidator());
	}

	private MeasureReportServiceImpl measureReportServiceImpl()
	{
		return new MeasureReportServiceImpl(MeasureReportServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.measureReportDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public NamingSystemService namingSystemService()
	{
		return new NamingSystemServiceJaxrs(namingSystemServiceSecure());
	}

	private NamingSystemServiceSecure namingSystemServiceSecure()
	{
		return new NamingSystemServiceSecure(namingSystemServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.namingSystemDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.namingSystemAuthorizationRule(), validationConfig.resourceValidator());
	}

	private NamingSystemService namingSystemServiceImpl()
	{
		return new NamingSystemServiceImpl(NamingSystemServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.namingSystemDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public OrganizationService organizationService()
	{
		return new OrganizationServiceJaxrs(organizationServiceSecure());
	}

	private OrganizationServiceSecure organizationServiceSecure()
	{
		return new OrganizationServiceSecure(organizationServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.organizationDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.organizationAuthorizationRule(), validationConfig.resourceValidator());
	}

	private OrganizationServiceImpl organizationServiceImpl()
	{
		return new OrganizationServiceImpl(OrganizationServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.organizationDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public OrganizationAffiliationService organizationAffiliationService()
	{
		return new OrganizationAffiliationServiceJaxrs(organizationAffiliationServiceSecure());
	}

	private OrganizationAffiliationServiceSecure organizationAffiliationServiceSecure()
	{
		return new OrganizationAffiliationServiceSecure(organizationAffiliationServiceImpl(),
				propertiesConfig.getServerBaseUrl(), helperConfig.responseGenerator(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				referenceConfig.referenceExtractor(), daoConfig.organizationAffiliationDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.organizationAffiliationAuthorizationRule(), validationConfig.resourceValidator());
	}

	private OrganizationAffiliationServiceImpl organizationAffiliationServiceImpl()
	{
		return new OrganizationAffiliationServiceImpl(OrganizationAffiliationServiceJaxrs.PATH,
				propertiesConfig.getServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				daoConfig.organizationAffiliationDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public PatientService patientService()
	{
		return new PatientServiceJaxrs(patientServiceSecure());
	}

	private PatientServiceSecure patientServiceSecure()
	{
		return new PatientServiceSecure(patientServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.patientDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.patientAuthorizationRule(), validationConfig.resourceValidator());
	}

	private PatientServiceImpl patientServiceImpl()
	{
		return new PatientServiceImpl(PatientServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.patientDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public PractitionerRoleService practitionerRoleService()
	{
		return new PractitionerRoleServiceJaxrs(practitionerRoleServiceSecure());
	}

	private PractitionerRoleServiceSecure practitionerRoleServiceSecure()
	{
		return new PractitionerRoleServiceSecure(practitionerRoleServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(),
				daoConfig.practitionerRoleDao(), helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.practitionerRoleAuthorizationRule(), validationConfig.resourceValidator());
	}

	private PractitionerRoleServiceImpl practitionerRoleServiceImpl()
	{
		return new PractitionerRoleServiceImpl(PractitionerRoleServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.practitionerRoleDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public PractitionerService practitionerService()
	{
		return new PractitionerServiceJaxrs(practitionerServiceSecure());
	}

	private PractitionerServiceSecure practitionerServiceSecure()
	{
		return new PractitionerServiceSecure(practitionerServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.practitionerDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.practitionerAuthorizationRule(), validationConfig.resourceValidator());
	}

	private PractitionerServiceImpl practitionerServiceImpl()
	{
		return new PractitionerServiceImpl(PractitionerServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.practitionerDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public ProvenanceService provenanceService()
	{
		return new ProvenanceServiceJaxrs(provenanceServiceSecure());
	}

	private ProvenanceServiceSecure provenanceServiceSecure()
	{
		return new ProvenanceServiceSecure(provenanceServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.provenanceDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.provenanceAuthorizationRule(), validationConfig.resourceValidator());
	}

	private ProvenanceServiceImpl provenanceServiceImpl()
	{
		return new ProvenanceServiceImpl(ProvenanceServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.provenanceDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public QuestionnaireService questionnaireService()
	{
		return new QuestionnaireServiceJaxrs(questionnaireServiceSecure());
	}

	private QuestionnaireServiceSecure questionnaireServiceSecure()
	{
		return new QuestionnaireServiceSecure(questionnaireServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.questionnaireDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.questionnaireAuthorizationRule(), validationConfig.resourceValidator());
	}

	private QuestionnaireServiceImpl questionnaireServiceImpl()
	{
		return new QuestionnaireServiceImpl(QuestionnaireServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.questionnaireDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public QuestionnaireResponseService questionnaireResponseService()
	{
		return new QuestionnaireResponseServiceJaxrs(questionnaireResponseServiceSecure());
	}

	private QuestionnaireResponseServiceSecure questionnaireResponseServiceSecure()
	{
		return new QuestionnaireResponseServiceSecure(questionnaireResponseServiceImpl(),
				propertiesConfig.getServerBaseUrl(), helperConfig.responseGenerator(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				referenceConfig.referenceExtractor(), daoConfig.questionnaireResponseDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.questionnaireResponseAuthorizationRule(), validationConfig.resourceValidator());
	}

	private QuestionnaireResponseServiceImpl questionnaireResponseServiceImpl()
	{
		return new QuestionnaireResponseServiceImpl(QuestionnaireResponseServiceJaxrs.PATH,
				propertiesConfig.getServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				daoConfig.questionnaireResponseDao(), validationConfig.resourceValidator(), eventConfig.eventManager(),
				helperConfig.exceptionHandler(), eventConfig.eventGenerator(), helperConfig.responseGenerator(),
				helperConfig.parameterConverter(), referenceConfig.referenceExtractor(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				authorizationConfig.authorizationRuleProvider(), historyConfig.historyService());
	}

	@Bean
	public ResearchStudyService researchStudyService()
	{
		return new ResearchStudyServiceJaxrs(researchStudyServiceSecure());
	}

	private ResearchStudyServiceSecure researchStudyServiceSecure()
	{
		return new ResearchStudyServiceSecure(researchStudyServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.researchStudyDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.researchStudyAuthorizationRule(), validationConfig.resourceValidator());
	}

	private ResearchStudyServiceImpl researchStudyServiceImpl()
	{
		return new ResearchStudyServiceImpl(ResearchStudyServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.researchStudyDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public StructureDefinitionService structureDefinitionService()
	{
		return new StructureDefinitionServiceJaxrs(structureDefinitionServiceSecure());
	}

	private StructureDefinitionServiceSecure structureDefinitionServiceSecure()
	{
		return new StructureDefinitionServiceSecure(structureDefinitionServiceImpl(),
				propertiesConfig.getServerBaseUrl(), helperConfig.responseGenerator(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				referenceConfig.referenceExtractor(), daoConfig.structureDefinitionDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.structureDefinitionAuthorizationRule(), validationConfig.resourceValidator());
	}

	private StructureDefinitionServiceImpl structureDefinitionServiceImpl()
	{
		return new StructureDefinitionServiceImpl(StructureDefinitionServiceJaxrs.PATH,
				propertiesConfig.getServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				daoConfig.structureDefinitionDao(), validationConfig.resourceValidator(), eventConfig.eventManager(),
				helperConfig.exceptionHandler(), eventConfig.eventGenerator(), helperConfig.responseGenerator(),
				helperConfig.parameterConverter(), referenceConfig.referenceExtractor(),
				referenceConfig.referenceResolver(), referenceConfig.referenceCleaner(),
				authorizationConfig.authorizationRuleProvider(), daoConfig.structureDefinitionSnapshotDao(),
				snapshotConfig.snapshotGenerator(), historyConfig.historyService());
	}

	@Bean
	public SubscriptionService subscriptionService()
	{
		return new SubscriptionServiceJaxrs(subscriptionServiceSecure());
	}

	private SubscriptionServiceSecure subscriptionServiceSecure()
	{
		return new SubscriptionServiceSecure(subscriptionServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.subscriptionDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.subscriptionAuthorizationRule(), validationConfig.resourceValidator());
	}

	private SubscriptionServiceImpl subscriptionServiceImpl()
	{
		return new SubscriptionServiceImpl(SubscriptionServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.subscriptionDao(),
				validationConfig.resourceValidator(), eventConfig.eventManager(), helperConfig.exceptionHandler(),
				eventConfig.eventGenerator(), helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public TaskService taskService()
	{
		return new TaskServiceJaxrs(taskServiceSecure());
	}

	private TaskServiceSecure taskServiceSecure()
	{
		return new TaskServiceSecure(taskServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.taskDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.taskAuthorizationRule(), validationConfig.resourceValidator());
	}

	private TaskServiceImpl taskServiceImpl()
	{
		return new TaskServiceImpl(TaskServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.taskDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public ValueSetService valueSetService()
	{
		return new ValueSetServiceJaxrs(valueSetServiceSecure());
	}

	private ValueSetServiceSecure valueSetServiceSecure()
	{
		return new ValueSetServiceSecure(valueSetServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), referenceConfig.referenceExtractor(), daoConfig.valueSetDao(),
				helperConfig.exceptionHandler(), helperConfig.parameterConverter(),
				authorizationConfig.valueSetAuthorizationRule(), validationConfig.resourceValidator());
	}

	private ValueSetServiceImpl valueSetServiceImpl()
	{
		return new ValueSetServiceImpl(ValueSetServiceJaxrs.PATH, propertiesConfig.getServerBaseUrl(),
				propertiesConfig.getDefaultPageCount(), daoConfig.valueSetDao(), validationConfig.resourceValidator(),
				eventConfig.eventManager(), helperConfig.exceptionHandler(), eventConfig.eventGenerator(),
				helperConfig.responseGenerator(), helperConfig.parameterConverter(),
				referenceConfig.referenceExtractor(), referenceConfig.referenceResolver(),
				referenceConfig.referenceCleaner(), authorizationConfig.authorizationRuleProvider(),
				historyConfig.historyService());
	}

	@Bean
	public RootService rootService()
	{
		return new RootServiceJaxrs(rootServiceSecure());
	}

	private RootServiceSecure rootServiceSecure()
	{
		return new RootServiceSecure(rootServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver(),
				authorizationConfig.rootAuthorizationRule());
	}

	private RootServiceImpl rootServiceImpl()
	{
		return new RootServiceImpl(commandConfig.commandFactory(), helperConfig.responseGenerator(),
				helperConfig.parameterConverter(), helperConfig.exceptionHandler(), referenceConfig.referenceCleaner(),
				historyConfig.historyService());
	}

	@Bean
	public ConformanceService conformanceService()
	{
		return new ConformanceServiceJaxrs(conformanceServiceSecure());
	}

	private ConformanceServiceSecure conformanceServiceSecure()
	{
		return new ConformanceServiceSecure(conformanceServiceImpl(), propertiesConfig.getServerBaseUrl(),
				helperConfig.responseGenerator(), referenceConfig.referenceResolver());
	}

	private ConformanceServiceImpl conformanceServiceImpl()
	{
		return new ConformanceServiceImpl(propertiesConfig.getServerBaseUrl(), propertiesConfig.getDefaultPageCount(),
				buildInfoReaderConfig.buildInfoReader(), helperConfig.parameterConverter(),
				validationConfig.validationSupport(),
				propertiesConfig.getOidcAuthorizationCodeFlowEnabled() || propertiesConfig.getOidcBearerTokenEnabled());
	}

	@Bean
	public StaticResourcesService staticResourcesService()
	{
		return new StaticResourcesService("/fhir", propertiesConfig.getStaticResourceCacheEnabled());
	}

	@Bean
	public StatusService statusService()
	{
		return new StatusService(daoConfig.dataSource(), propertiesConfig.getJettyStatusConnectorPort());
	}
}
