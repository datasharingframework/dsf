function startProcess() {
	const task = getResourceAsJson()
	const errors = []

	readTaskInputsFromForm(task, errors)

	if (errors.length === 0) {
		const taskString = JSON.stringify(task)
		createTask(taskString)
	}
}

function readTaskInputsFromForm(task, errors) {
	delete task["id"]
	delete task.meta["lastUpdated"]
	delete task.meta["version"]
	delete task.meta["versionId"]
	delete task["identifier"]

	// TODO set requester as practitioner-identifier if OIDC or Personal Client-Certificate
	//task.requester.type = "Practitioner"
	//task.requester.identifier.value = ""
	//task.requester.identifier.system = "http://dsf.dev/sid/practitioner-identifier"

	task.status = "requested"
	task.authoredOn = new Date().toISOString()

	const newInputs = []

	task.input.forEach((input) => {
		if (input.hasOwnProperty("type")) {
			const code = input.type.coding[0].code

			if (code !== "message-name" && code !== "business-key" && code !== "correlation-key") {
				document.querySelectorAll("div[name='" + code + "-input-row']").forEach(rowElement => {
					const newInput = JSON.parse(JSON.stringify(input)) // clone
					const inputValueType = Object.keys(newInput).find((string) => string.startsWith("value"))
					const inputValue = readAndValidateValue(rowElement, newInput, code, inputValueType, errors)

					if (inputValue) {
						newInput[inputValueType] = inputValue
						newInputs.push(newInput)
					}
				})
			} else {
				newInputs.push(input)
			}
		}
	})

	task.input = newInputs
}

function completeQuestionnaireResponse() {
	const questionnaireResponse = getResourceAsJson()
	const errors = []

	readQuestionnaireResponseAnswersFromForm(questionnaireResponse, errors)

	if (errors.length === 0) {
		const questionnaireResponseString = JSON.stringify(questionnaireResponse)
		updateQuestionnaireResponse(questionnaireResponseString)
	}
}

function readQuestionnaireResponseAnswersFromForm(questionnaireResponse, errors) {
	questionnaireResponse.status = "completed"

	const newItems = []

	questionnaireResponse.item.forEach((item) => {
		if (item.hasOwnProperty("answer")) {
			const id = item.linkId

			if (id !== "business-key" && id !== "user-task-id") {
				document.querySelectorAll("div[name='" + id + "-input-row']").forEach(rowElement => {
					const newItem = JSON.parse(JSON.stringify(item)) // clone
					const answer = newItem.answer[0]
					const answerType = Object.keys(answer).find((string) => string.startsWith("value"))
					const answerValue = readAndValidateValue(rowElement, answer, id, answerType, errors)

					if (answerValue) {
						answer[answerType] = answerValue
						newItems.push(newItem)
					}
				})
			} else {
				newItems.push(item)
			}
		}
	})

	questionnaireResponse.item = newItems
}

function readAndValidateValue(rowElement, templateValue, name, valueType, errors) {
	const valueElement = rowElement.querySelector("input[name='" + name + "']")
	const value = valueElement?.value
	const valueBoolean = rowElement.querySelector("input[name='" + name + "']:checked")?.value
	const valueValue = rowElement.querySelector("input[name='" + name + "-code']")?.value
	const valueSystem = rowElement.querySelector("input[name='" + name + "-system']")?.value

	const optional = rowElement.hasAttribute("optional")
	const valueExists = ((value && valueType !== "valueBoolean") ||
		(valueBoolean && valueType === "valueBoolean") ||
		valueValue || valueSystem)

	if (optional && !valueExists) {
		return null
	}

	const errorListElement = rowElement.querySelector("ul[name='" + name + "-error']")
	errorListElement.replaceChildren()

	if (valueType === 'valueString') {
		return validateString(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueInteger') {
		return validateInteger(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueDecimal') {
		return validateDecimal(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueDate') {
		return validateDate(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueTime') {
		return validateTime(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueDateTime') {
		return validateDateTime(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueInstant') {
		return validateInstant(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueUri') {
		return validateUrl(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueUrl') {
		return validateUrl(rowElement, errorListElement, value, errors)
	} else if (valueType === 'valueReference') {
		if (valueElement) {
			return validateReference(rowElement, errorListElement, value, errors)
		} else {
			const valueIdentifier = validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors)
			return { identifier: valueIdentifier, type: templateValue?.valueReference?.type }
		}
	} else if (valueType === 'valueBoolean') {
		return validateBoolean(rowElement, errorListElement, valueBoolean, errors)
	} else if (valueType === "valueIdentifier") {
		return validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors)
	} else if (valueType === "valueCoding") {
		return validateCoding(rowElement, errorListElement, valueSystem, valueValue, errors)
	} else {
		return null
	}
}

function validateString(rowElement, errorListElement, value, errors) {
	if (value === null || value.trim() === "") {
		addError(rowElement, errorListElement, errors, "Value is null or empty")
		return null
	} else {
		removeError(rowElement, errorListElement)
		return value
	}
}

function validateInteger(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	if (!Number.isInteger(parseInt(value))) {
		addError(rowElement, errorListElement, errors, "Value is not an integer")
		return null
	} else {
		removeError(rowElement, errorListElement)
		return value
	}
}

function validateDecimal(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	if (isNaN(parseFloat(value))) {
		addError(rowElement, errorListElement, errors, "Value is not a decimal")
		return null
	} else {
		removeError(rowElement, errorListElement)
		return value
	}
}

function validateDate(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	const date = new Date(value)
	if ((date === "Invalid Date") || isNaN(date)) {
		addError(rowElement, errorListElement, errors, "Value is not a date")
		return null
	} else {
		removeError(rowElement, errorListElement)
		return value
	}
}

function validateTime(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	if (!(new RegExp('^(2[0-3]|[01]?[0-9]):([0-5]?[0-9])(:[0-5]?[0-9])?$').test(value))) {
		addError(rowElement, errorListElement, errors, "Value is not a time")
		return null
	} else {
		removeError(rowElement, errorListElement)
		return (value.split(":").length - 1 === 1) ? value + ":00" : value
	}
}

function validateDateTime(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	try {
		const dateTime = new Date(value).toISOString()
		removeError(rowElement, errorListElement)
		return dateTime
	} catch (_) {
		addError(rowElement, errorListElement, errors, "Value is not a date time")
		return null
	}
}

function validateInstant(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	try {
		const dateTime = new Date(value).toISOString()
		removeError(rowElement, errorListElement)
		return dateTime
	} catch (_) {
		addError(rowElement, errorListElement, errors, "Value is not an instant")
		return null
	}
}

function validateReference(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	try {
		new URL(value)
		removeError(rowElement, errorListElement)
		return { reference: value }
	} catch (_) {
		addError(rowElement, errorListElement, errors, "Value is not a reference")
		return null
	}
}

function validateUrl(rowElement, errorListElement, value, errors) {
	validateString(rowElement, errorListElement, value, errors)

	try {
		new URL(value)
		removeError(rowElement, errorListElement)
		return value
	} catch (_) {
		addError(rowElement, errorListElement, errors, "Value is not a url")
		return null
	}
}

function validateBoolean(rowElement, errorListElement, valueBoolean, errors) {
	if (valueBoolean === "true" || valueBoolean === "false") {
		removeError(rowElement, errorListElement)
		return valueBoolean
	} else {
		addError(rowElement, errorListElement, errors, "Boolean value not selected")
		return null
	}
}

function validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors) {
	const validatedSystem = validateUrl(rowElement, errorListElement, valueSystem, errors)
	const validatedValue = validateString(rowElement, errorListElement, valueValue, errors)

	if (validatedSystem && validatedValue) {
		removeError(rowElement, errorListElement)
		return { system: valueSystem, value: valueValue }
	} else {
		addError(rowElement, errorListElement, errors, "System or value not usable for identifier")
		return null
	}
}

function validateCoding(rowElement, errorListElement, valueSystem, valueValue, errors) {
	const validatedSystem = validateUrl(rowElement, errorListElement, valueSystem, errors)
	const validatedCode = validateString(rowElement, errorListElement, valueValue, errors)

	if (validatedSystem && validatedCode) {
		removeError(rowElement, errorListElement)
		return { system: valueSystem, code: valueValue }
	} else {
		addError(rowElement, errorListElement, errors, "System or code not usable for coding")
		return null
	}
}

function addError(rowElement, errorListElement, errors, message) {
	const id = rowElement.getAttribute("name") + "-" + rowElement.getAttribute("index")
	errors.push({ id: id, error: message })

	rowElement.classList.add("error")

	const errorMessageElement = document.createElement("li")
	errorMessageElement.appendChild(document.createTextNode(message))

	errorListElement.appendChild(errorMessageElement)
	errorListElement.classList.remove("error-list-not-visible")
	errorListElement.classList.add("error-list-visible")
}

function removeError(rowElement, errorListElement) {
	rowElement.classList.remove("error")

	errorListElement.classList.remove("error-list-visible")
	errorListElement.classList.add("error-list-not-visible")
	errorListElement.replaceChildren()
}

function updateQuestionnaireResponse(questionnaireResponse) {
	const fullUrl = window.location.origin + window.location.pathname
	const requestUrl = fullUrl.indexOf("/_history") < 0 ? fullUrl : fullUrl.slice(0, fullUrl.indexOf("/_history"))
	const resourceBaseUrlWithoutId = fullUrl.slice(0, fullUrl.indexOf("/QuestionnaireResponse") + "/QuestionnaireResponse".length)

	enableSpinner()

	fetch(requestUrl, {
		method: "PUT",
		headers: {
			'Content-type': 'application/json',
			'Accept': 'application/json'
		},
		body: questionnaireResponse
	}).then(response => {
		parseResponse(response, resourceBaseUrlWithoutId)
	})
}

function createTask(task) {
	const fullUrl = window.location.origin + window.location.pathname
	const requestUrl = fullUrl.slice(0, fullUrl.indexOf("/Task") + "/Task".length)

	enableSpinner()

	fetch(requestUrl, {
		method: "POST",
		headers: {
			'Content-type': 'application/json',
			'Accept': 'application/json'
		},
		body: task
	}).then(response => {
		parseResponse(response, requestUrl)
	})
}

function parseResponse(response, resourceBaseUrlWithoutId) {
	response.text().then((text) => {
		if (response.ok) {
			const resource = JSON.parse(text)
			setTimeout(() => {
				disableSpinner()
				window.location.href = resourceBaseUrlWithoutId + "/" + resource.id
			}, 1000)
		} else {
			disableSpinner()
			const statusText = response.statusText === null ? " - " + response.statusText : ""
			window.alert("Status: " + response.status + statusText + "\n\n" + text)
		}
	})
}

function enableSpinner() {
	const spinner = document.getElementById("spinner")
	spinner.classList.remove("spinner-disabled")
	spinner.classList.add("spinner-enabled")
}

function disableSpinner() {
	const spinner = document.getElementById("spinner")
	spinner.classList.remove("spinner-enabled")
	spinner.classList.add("spinner-disabled")
}

function adaptTaskFormInputs(resourceType) {
	if (resourceType !== null && resourceType[1] !== undefined && resourceType[1] === 'Task') {
		const task = getResourceAsJson()

		if (task.status === 'draft' && task.meta !== null && task.meta.profile !== null && task.meta.profile.length > 0) {
			const profile = task.meta.profile[0]

			let currentUrl = window.location.origin + window.location.pathname
			let requestUrl = currentUrl.slice(0, currentUrl.indexOf("/Task")) + "/StructureDefinition?url=" + profile

			loadResource(requestUrl).then(bundle => parseStructureDefinition(bundle))
		}
	}
}

function adaptQuestionnaireResponseInputsIfNotVersion1_0_0(resourceType) {
	if (resourceType !== null && resourceType[1] !== undefined && resourceType[1] === 'QuestionnaireResponse') {
		const questionnaireResponse = getResourceAsJson()

		if (questionnaireResponse.status === 'in-progress' && questionnaireResponse.questionnaire !== null) {
			const urlVersion = questionnaireResponse.questionnaire.split('|')

			if (urlVersion.length > 1) {
				const url = urlVersion[0]
				const version = urlVersion[1]

				let currentUrl = window.location.origin + window.location.pathname
				let requestUrl = currentUrl.slice(0, currentUrl.indexOf("/QuestionnaireResponse")) + "/Questionnaire?url=" + url + '&version=' + version

				loadResource(requestUrl).then(bundle => parseQuestionnaire(bundle))
			}
		}
	}
}

function loadResource(url) {
	return fetch(url, {
		method: "GET",
		headers: {
			'Accept': 'application/json'
		}
	}).then(response => response.json())
}

function parseStructureDefinition(bundle) {
	if (bundle.entry.length > 0 && bundle.entry[0].resource !== null) {
		const structureDefinition = bundle.entry[0].resource

		if (structureDefinition.differential !== null) {
			const differentials = structureDefinition.differential.element
			const slices = filterInputSlices(differentials)
			const groupedSlices = groupBy(slices, d => d.id.split(".")[1])
			const definitions = getDefinitions(groupedSlices)

			const indices = new Map()
			definitions.forEach(definition => { modifyTaskInputRow(definition, indices) })
		}
	}
}

function parseQuestionnaire(bundle) {
	if (bundle.entry.length > 0 && bundle.entry[0].resource !== null) {
		const questionnaire = bundle.entry[0].resource

		if (questionnaire.meta !== null && questionnaire.meta.profile !== null && questionnaire.meta.profile.length > 0) {
			const profile = questionnaire.meta.profile[0]
			const urlVersion = profile.split('|')

			if (urlVersion.length > 1) {
				const url = urlVersion[0]
				const version = urlVersion[1]

				if (version !== '1.0.0' && questionnaire.item !== undefined) {
					questionnaire.item.forEach(item => { modifyQuestionnaireInputRow(item) })
				}
			}
		}
	}
}

function filterInputSlices(differentials) {
	return differentials.filter(diff => diff.id.startsWith("Task.input:")
		&& !(diff.id.includes("message-name") || diff.id.includes("business-key")
			|| diff.id.includes("correlation-key")))
}

function groupBy(list, keyGetter) {
	const map = new Map()

	list.forEach((item) => {
		const key = keyGetter(item)
		const collection = map.get(key)

		if (!collection) {
			map.set(key, [item])
		} else {
			collection.push(item)
		}
	})

	return Array.from(map.values())
}

function getDefinitions(groupedSlices) {
	return groupedSlices.map(differentials => {
		const valueType = getValueOfDifferential(differentials, "Task.input.value[x]", "type")

		return {
			identifier: window.location.href,
			typeSystem: getValueOfDifferential(differentials, "Task.input.type.coding.system", "fixedUri"),
			typeCode: getValueOfDifferential(differentials, "Task.input.type.coding.code", "fixedCode"),
			valueType: (valueType !== undefined && valueType.length > 0) ? valueType[0].code : undefined,
			min: getValueOfDifferential(differentials, "Task.input", "min"),
			max: getValueOfDifferential(differentials, "Task.input", "max"),
		}
	})
}

function getValueOfDifferential(differentials, path, property) {
	const values = differentials.filter(d => d.path !== null && d.path === path)

	if (values.length > 0) {
		return values[0][property]
	} else {
		return undefined
	}
}

function modifyTaskInputRow(definition, indices) {
	const row = document.querySelector("[name='" + definition.typeCode + "-input-row']")

	if (row) {
		const rowIndex = row.getAttribute("index")
		if (rowIndex) {
			const index = parseInt(rowIndex)
			indices.set(getDefinitionId(definition), index)
		}

		const label = row.querySelector("label")
		if (label) {
			const cardinalities = htmlToElement('<span class="cardinalities"></span>', ' [' + definition.min + '..' + definition.max + ']')
			label.appendChild(cardinalities)

			if (definition.max !== "1") {
				const plusIcon = htmlToElement('<span class="plus-minus-icon"></span>')
				const plusIconSvg = htmlToElement('<svg height="20" width="20" viewBox="0 -960 960 960"><title>Add additional input</title><path d="M453-280h60v-166h167v-60H513v-174h-60v174H280v60h173v166Zm27.266 200q-82.734 0-155.5-31.5t-127.266-86q-54.5-54.5-86-127.341Q80-397.681 80-480.5q0-82.819 31.5-155.659Q143-709 197.5-763t127.341-85.5Q397.681-880 480.5-880q82.819 0 155.659 31.5Q709-817 763-763t85.5 127Q880-563 880-480.266q0 82.734-31.5 155.5T763-197.684q-54 54.316-127 86Q563-80 480.266-80Zm.234-60Q622-140 721-239.5t99-241Q820-622 721.188-721 622.375-820 480-820q-141 0-240.5 98.812Q140-622.375 140-480q0 141 99.5 240.5t241 99.5Zm-.5-340Z"/></svg>')

				plusIconSvg.addEventListener("click", () => {
					appendInputRowAfter(row, definition, indices)
				})

				plusIcon.appendChild(plusIconSvg)
				label.appendChild(plusIcon)
			}
		}

		if (definition.min < 1 || definition.min === undefined)
			row.setAttribute("optional", "")
	}
}

function modifyQuestionnaireInputRow(item) {
	const row = document.querySelector("[name='" + item.linkId + "-input-row']")

	if (row) {
		if (item.required !== true) {
			row.setAttribute("optional", "")
		}

		const label = row.querySelector("label")
		if (label) {
			const cardinalities = htmlToElement('<span class="cardinalities"></span>', ' [' + (item.required === true ? '1': '0') + '..1]')
			label.appendChild(cardinalities)
		}
	}

	if (item.item) {
		item.item.forEach(item => { modifyQuestionnaireInputRow(item) })
	}
}

function appendInputRowAfter(inputRow, definition, indices) {
	const clone = inputRow.cloneNode(true)

	const index = getIndex(getDefinitionId(definition), indices)
	clone.setAttribute("index", index)
	clone.querySelectorAll("[index]").forEach(e => { e.setAttribute("index", index) })

	clone.querySelector("span[class='plus-minus-icon']").remove()
	clone.querySelectorAll("input").forEach(input => {

		input.value = ''

		if (input?.nextElementSibling?.tagName?.toLowerCase() === 'svg') {
			input.nextElementSibling.addEventListener('click', () => {
				if (input?.placeholder !== '') {
					input.text = input.placeholder
					input.value = input.placeholder
				}
			})
		}
	})

	const label = clone.querySelector("label")
	if (label) {
		const minusIcon = htmlToElement('<span class="plus-minus-icon"></span>')
		const minusIconSvg = htmlToElement('<svg height="20" width="20" viewBox="0 -960 960 960"><path d="M280-453h400v-60H280v60ZM480-80q-82 0-155-31.5t-127.5-86Q143-252 111.5-325T80-480q0-83 31.5-156t86-127Q252-817 325-848.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 82-31.5 155T763-197.5q-54 54.5-127 86T480-80Zm0-60q142 0 241-99.5T820-480q0-142-99-241t-241-99q-141 0-240.5 99T140-480q0 141 99.5 240.5T480-140Zm0-340Z"/></svg>')

		minusIconSvg.addEventListener("click", () => { clone.remove() })

		minusIcon.appendChild(minusIconSvg)
		label.appendChild(minusIcon)
	}

	inputRow.after(clone)
}

function insertPlaceholderInValue(element, name, placeholder) {
	const input = element.querySelector("input[name='" + name + "']")
	input.text = placeholder
	input.value = placeholder
}

function htmlToElement(html, innerText) {
	const template = document.createElement('template')
	template.innerHTML = html
	const child = template.content.firstChild

	if (innerText)
		child.innerText = innerText

	return child
}

function getDefinitionId(definition) {
	return definition.typeSystem + "|" + definition.typeCode
}

function getIndex(id, indexMap) {
	if (indexMap.has(id)) {
		const index = indexMap.get(id) + 1
		indexMap.set(id, index)
		return index
	} else {
		indexMap.set(id, 0)
		return 0
	}
}

function getResourceAsJson() {
	const resource = document.getElementById("json").innerText
	return JSON.parse(resource)
}