/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function showBookmarks() {
	const bookmarks = document.getElementById('bookmarks')
	bookmarks.style.display = 'block'

	const click = e => {
		if (!bookmarks.contains(e.target) && !document.getElementById('bookmark-list').contains(e.target)) {
			closeBookmarks()
			document.removeEventListener('click', click)
		}
	}
	document.addEventListener('click', click)
}

function closeBookmarks() {
	checkBookmarked()
	createBookmarkList(getBookmarks())

	const bookmarks = document.getElementById('bookmarks')
	bookmarks.style.display = 'none'
}

function addCurrentBookmark() {
	const addIcon = document.getElementById('bookmark-add')
	addIcon.style.display = 'none'
	const removeIcon = document.getElementById('bookmark-remove')
	removeIcon.style.display = 'inline'

	const url = window.location.pathname.substring(new URL(document.head.baseURI).pathname.length) + window.location.search
	const resourceType = getResourceTypeOrMisc(url)

	const bookmarks = getBookmarks()

	const resourceTypeBookmarks = bookmarks[resourceType] !== undefined ? bookmarks[resourceType] : []
	resourceTypeBookmarks.push(url)
	bookmarks[resourceType] = resourceTypeBookmarks.sort()

	saveBookmarks(bookmarks)
	createBookmarkList(bookmarks)
}

function removeCurrentBookmark() {
	const addIcon = document.getElementById('bookmark-add')
	addIcon.style.display = 'inline'
	const removeIcon = document.getElementById('bookmark-remove')
	removeIcon.style.display = 'none'

	const url = window.location.pathname.substring(new URL(document.head.baseURI).pathname.length) + window.location.search
	const resourceType = getResourceTypeOrMisc(url)

	const bookmarks = getBookmarks()

	const resourceTypeBookmarks = bookmarks[resourceType] !== undefined ? bookmarks[resourceType] : []
	bookmarks[resourceType] = resourceTypeBookmarks.filter(item => item !== url).sort()

	saveBookmarks(bookmarks)
	createBookmarkList(bookmarks)
}

function checkBookmarked() {
	const url = window.location.pathname.substring(new URL(document.head.baseURI).pathname.length) + window.location.search
	const resourceType = getResourceTypeOrMisc(url)

	const addIcon = document.getElementById('bookmark-add')
	const removeIcon = document.getElementById('bookmark-remove')

	const bookmarks = getBookmarks()
	if (bookmarks[resourceType] !== undefined && bookmarks[resourceType].includes(url)) {
		addIcon.style.display = "none"
		removeIcon.style.display = "inline"
	}
	else {
		addIcon.style.display = "inline"
		removeIcon.style.display = "none"
	}

	createBookmarkList(bookmarks)
}

function getBookmarks() {
	if (localStorage != null) {
		return localStorage.getItem('bookmarks') != null && localStorage.getItem('bookmarks') != "" ? cleanBookmarkUrls(JSON.parse(localStorage.getItem('bookmarks'))) : getInitialBookmarks()
	}
	else {
		alert("session storage not available")
	}
}

function cleanBookmarkUrls(bookmarks) {
	let b = new Object()
	Object.entries(bookmarks).map(e => new Array(e[0], e[1].map(u => u.startsWith('/fhir/') ? u.substring('/fhir/'.length) : u))).forEach(e => b[e[0].startsWith('$') ? e[0] : '$' + e[0]] = e[1])
	return b
}

function saveBookmarks(bookmarks) {
	if (localStorage != null) {
		localStorage.setItem('bookmarks', JSON.stringify(bookmarks))
	}
	else {
		alert("session storage not available")
	}
}

function getResourceTypeOrMisc(url) {
	const regex = new RegExp('(?:(?:http|https):\/\/(?:[A-Za-z0-9-.:%$]*\/)+)?'
		+ '(Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse|AuditEvent|Basic|Binary|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement'
		+ '|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition'
		+ '|ConceptMap|Condition|Consent|Contract|Coverage|CoverageEligibilityRequest|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest|DeviceUseStatement'
		+ '|DiagnosticReport|DocumentManifest|DocumentReference|EffectEvidenceSynthesis|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|EventDefinition|Evidence|EvidenceVariable'
		+ '|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingStudy|Immunization|ImmunizationEvaluation'
		+ '|ImmunizationRecommendation|ImplementationGuide|InsurancePlan|Invoice|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense'
		+ '|MedicationKnowledge|MedicationRequest|MedicationStatement|MedicinalProduct|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication|MedicinalProductIngredient'
		+ '|MedicinalProductInteraction|MedicinalProductManufactured|MedicinalProductPackaged|MedicinalProductPharmaceutical|MedicinalProductUndesirableEffect|MessageDefinition|MessageHeader'
		+ '|MolecularSequence|NamingSystem|NutritionOrder|Observation|ObservationDefinition|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation|Patient|PaymentNotice'
		+ '|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup|ResearchDefinition'
		+ '|ResearchElementDefinition|ResearchStudy|ResearchSubject|RiskAssessment|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot|Specimen|SpecimenDefinition|StructureDefinition'
		+ '|StructureMap|Subscription|Substance|SubstanceNucleicAcid|SubstancePolymer|SubstanceProtein|SubstanceReferenceInformation|SubstanceSourceMaterial|SubstanceSpecification|SupplyDelivery'
		+ '|SupplyRequest|Task|TerminologyCapabilities|TestReport|TestScript|ValueSet|VerificationResult|VisionPrescription)'
		+ '(?:[\/?#](?:.*)?)?$')
	const match = regex.exec(url)
	if (match != null)
		return '$' + match[1]
	else
		return '$_misc'
}

function getInitialBookmarks() {
	return {
		'$_misc': ['metadata'],
		'$ActivityDefinition': ['ActivityDefinition', "ActivityDefinition?_sort=status,url,version"],
		'$Binary': ['Binary'],
		'$CodeSystem': ['CodeSystem'],
		'$DocumentReference': ['DocumentReference'],
		'$Endpoint': ['Endpoint'],
		'$NamingSystem': ['NamingSystem'],
		'$Library': ['Library'],
		'$Measure': ['Measure'],
		'$MeasureReport': ['MeasureReport'],
		'$Organization': ['Organization', 'Organization?identifier=bzkf.de', 'Organization?identifier=medizininformatik-initiative.de', 'Organization?identifier=nct.dkfz.de', 'Organization?identifier=netzwerk-universitaetsmedizin.de'],
		'$OrganizationAffiliation': ['OrganizationAffiliation'],
		'$Questionnaire': ['Questionnaire'],
		'$QuestionnaireResponse': ['QuestionnaireResponse?_sort=-_lastUpdated', 'QuestionnaireResponse?_sort=-_lastUpdated&status=in-progress'],
		'$Subscription': ['Subscription'],
		'$StructureDefinition': ['StructureDefinition'],
		'$Task': ['Task', 'Task?_sort=-_lastUpdated', 'Task?_sort=_profile,identifier&status=draft'],
		'$ValueSet': ['ValueSet']
	}
}

function createBookmarkList(bookmarks) {
	let counter = 1
	const addIcon = document.getElementById('bookmark-add')
	const removeIcon = document.getElementById('bookmark-remove')
	const bookmarkList = document.getElementById('bookmarks-list')
	bookmarkList.innerHTML = null

	Object.entries(bookmarks).map(e => [e[0].substring(1), e[1]]).sort((e1, e2) => e1[0].localeCompare(e2[0])).forEach(e => {
		if (e[0] !== '_misc' && e[1].length > 0) {
			const h4 = document.createElement("h4")
			const h4Link = document.createElement("a")
			h4Link.href = e[0]
			h4Link.title = 'Open ' + e[0]
			const h4Content = document.createTextNode(e[0])
			h4.appendChild(h4Link)
			h4Link.appendChild(h4Content)
			bookmarkList.appendChild(h4)
		}
		if (e[1].length > 0) {
			e[1].filter(b => b !== e[0]).forEach(b => {
				const c = counter
				const div = document.createElement("div")
				div.setAttribute('id', 'bookmarks-list-entry-' + c)
				const divAddIcon = addIcon.cloneNode(true)
				divAddIcon.setAttribute('id', 'bookmark-add-' + c)
				divAddIcon.addEventListener('click', () => addBookmark(b, c))
				divAddIcon.setAttribute('viewBox', '4 0 24 24')
				divAddIcon.style.display = 'none'
				divAddIcon.children[0].innerHTML = 'Add Bookmark: ' + b
				const divRemoveIcon = removeIcon.cloneNode(true)
				divRemoveIcon.setAttribute('id', 'bookmark-remove-' + c)
				divRemoveIcon.addEventListener('click', () => removeBookmark(b, c))
				divRemoveIcon.setAttribute('viewBox', '4 0 24 24')
				divRemoveIcon.style.display = 'inline'
				divRemoveIcon.children[0].innerHTML = 'Remove Bookmark: ' + b
				const divLink = document.createElement("a")
				divLink.href = b
				divLink.title = 'Open ' + b
				const divContent = document.createTextNode(e[0] === '_misc' ? b : b.substring(e[0].length))
				div.appendChild(divAddIcon)
				div.appendChild(divRemoveIcon)
				div.appendChild(divLink)
				divLink.appendChild(divContent)
				bookmarkList.appendChild(div)

				counter++
			})
		}
	})
}

function removeBookmark(url, counter) {
	const addIcon = document.getElementById('bookmark-add-' + counter)
	addIcon.style.display = 'inline'
	const removeIcon = document.getElementById('bookmark-remove-' + counter)
	removeIcon.style.display = 'none'
	const div = document.getElementById('bookmarks-list-entry-' + counter)
	div.className = 'bookmarks-list-entry-removed'

	const resourceType = getResourceTypeOrMisc(url)

	const bookmarks = getBookmarks()

	const resourceTypeBookmarks = bookmarks[resourceType] !== undefined ? bookmarks[resourceType] : []
	bookmarks[resourceType] = resourceTypeBookmarks.filter(item => item !== url).sort()

	saveBookmarks(bookmarks)
}

function addBookmark(url, counter) {
	const addIcon = document.getElementById('bookmark-add-' + counter)
	addIcon.style.display = 'none'
	const removeIcon = document.getElementById('bookmark-remove-' + counter)
	removeIcon.style.display = 'inline'
	const div = document.getElementById('bookmarks-list-entry-' + counter)
	div.className = null

	const resourceType = getResourceTypeOrMisc(url)

	const bookmarks = getBookmarks()

	let resourceTypeBookmarks = bookmarks[resourceType] !== undefined ? bookmarks[resourceType] : []
	resourceTypeBookmarks.push(url)
	bookmarks[resourceType] = resourceTypeBookmarks.sort()

	saveBookmarks(bookmarks)
}