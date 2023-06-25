function startProcess() {
    const task = getResourceAsJson()
    const errors = []

    readTaskInputsFromForm(task, errors)

    console.log(task)
    console.log(errors)

    if (errors.length === 0) {
        const taskString = JSON.stringify(task)
        createTask(taskString)
    }
}

function readTaskInputsFromForm(task, errors) {
    task.id = null
    task.meta.lastUpdated = null
    task.meta.version = null

    // TODO set requester as practitioner-identifier if OIDC
    //task.requester.type = "Practitioner"
    //task.requester.identifier.value = ""
    //task.requester.identifier.system = "http://dsf.dev/sid/practitioner-identifier"

    task.status = "requested"
    task.authoredOn = new Date().toISOString()

    task.input.forEach((input) => {
        if (input.hasOwnProperty("type")) {
            const id = input.type.coding[0].code

            if (id !== "message-name" && id !== "business-key" && id !== "correlation-key") {
                const inputValueType = Object.keys(input).find((string) => string.startsWith("value"))
                input[inputValueType] = readAndValidateValue(input, id, inputValueType, errors)
            }
        }
    })
}

function completeQuestionnaireResponse() {
    const questionnaireResponse = getResourceAsJson()
    const errors = []

    readQuestionnaireResponseAnswersFromForm(questionnaireResponse, errors)

    console.log(questionnaireResponse)
    console.log(errors)

    if (errors.length === 0) {
        const questionnaireResponseString = JSON.stringify(questionnaireResponse)
        updateQuestionnaireResponse(questionnaireResponseString)
    }
}

function readQuestionnaireResponseAnswersFromForm(questionnaireResponse, errors) {
    questionnaireResponse.status = "completed"

    questionnaireResponse.item.forEach((item) => {
        if (item.hasOwnProperty("answer")) {
            const id = item.linkId

            if (id !== "business-key" && id !== "user-task-id") {
                const answer = item.answer[0]
                const answerType = Object.keys(answer).find((string) => string.startsWith("value"))

                answer[answerType] = readAndValidateValue(answer, id, answerType, errors)
            }
        }
    })
}

function readAndValidateValue(templateValue, id, valueType, errors) {
    const parentElement = document.getElementById(id)

    const value = parentElement?.value
    const valueSystem = document.getElementById(id + "-system")?.value
    const valueValue = document.getElementById(id + "-value")?.value

    const rowElement = document.getElementById(id + "-input-row")
    const errorListElement = document.getElementById(id + "-error")
    errorListElement.replaceChildren()

    if (valueType === 'valueString') {
        return validateString(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueInteger') {
        return validateInteger(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueDecimal') {
        return validateDecimal(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueDate') {
        return validateDate(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueTime') {
        return validateTime(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueDateTime') {
        return validateDateTime(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueInstant') {
        return validateInstant(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueUri') {
        return validateUrl(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueReference') {
        if (parentElement) {
            return validateReference(rowElement, errorListElement, value, errors, id)
        } else {
            const valueIdentifier = validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors, id)
            return {identifier: valueIdentifier, type: templateValue?.valueReference?.type}
        }
    } else if (valueType === 'valueBoolean') {
        return document.querySelector("input[name=" + id + "]:checked").value
    } else if (valueType === "valueIdentifier") {
        return validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors, id)
    } else if (valueType === "valueCoding") {
        return validateCoding(rowElement, errorListElement, valueSystem, valueValue, errors, id)
    } else {
        return null
    }
}

function validateString(rowElement, errorListElement, value, errors, id) {
    if (value === null || value.trim() === "") {
        addError(rowElement, errorListElement, errors, id, "Value is null or empty")
        return null
    } else {
        removeError(rowElement, errorListElement)
        return value
    }
}

function validateInteger(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    if (!Number.isInteger(parseInt(value))) {
        addError(rowElement, errorListElement, errors, id, "Value is not an integer")
        return null
    } else {
        removeError(rowElement, errorListElement)
        return value
    }
}

function validateDecimal(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    if (isNaN(parseFloat(value))) {
        addError(rowElement, errorListElement, errors, id, "Value is not a decimal")
        return null
    } else {
        removeError(rowElement, errorListElement)
        return value
    }
}

function validateDate(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    const date = new Date(value)
    if ((date === "Invalid Date") || isNaN(date)) {
        addError(rowElement, errorListElement, errors, id, "Value is not a date")
        return null
    } else {
        removeError(rowElement, errorListElement)
        return value
    }
}

function validateTime(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    if (!(new RegExp('^([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$').test(value))) {
        addError(rowElement, errorListElement, errors, id, "Value is not a time")
        return null
    } else {
        removeError(rowElement, errorListElement)
        return value + ":00"
    }
}

function validateDateTime(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    try {
        const dateTime = new Date(value).toISOString()
        removeError(rowElement, errorListElement)
        return dateTime
    } catch (_) {
        addError(rowElement, errorListElement, errors, id, "Value is not a date time")
        return null
    }
}

function validateInstant(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    try {
        const dateTime = new Date(value).toISOString()
        removeError(rowElement, errorListElement)
        return dateTime
    } catch (_) {
        addError(rowElement, errorListElement, errors, id, "Value is not an instant")
        return null
    }
}

function validateReference(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    try {
        new URL(value)
        removeError(rowElement, errorListElement)
        return {reference: value}
    } catch (_) {
        addError(rowElement, errorListElement, errors, id, "Value is not a reference")
        return null
    }
}

function validateUrl(rowElement, errorListElement, value, errors, id) {
    validateString(rowElement, errorListElement, value, errors, id)

    try {
        new URL(value)
        removeError(rowElement, errorListElement)
        return value
    } catch (_) {
        addError(rowElement, errorListElement, errors, id, "Value is not a url")
        return null
    }
}

function validateIdentifier(rowElement, errorListElement, valueSystem, valueValue, errors, id) {
    const validatedSystem = validateUrl(rowElement, errorListElement, valueSystem, errors, id)
    const validatedValue = validateString(rowElement, errorListElement, valueValue, errors, id)

    if (validatedSystem && validatedValue) {
         removeError(rowElement, errorListElement)
         return {system: valueSystem, value: valueValue}
    } else {
        addError(rowElement, errorListElement, errors, id, "System or value not usable for identifier")
        return null
    }
}

function validateCoding(rowElement, errorListElement, valueSystem, valueValue, errors, id) {
    const validatedSystem = validateUrl(rowElement, errorListElement, valueSystem, errors, id)
    const validatedCode = validateString(rowElement, errorListElement, valueValue, errors, id)

    if (validatedSystem && validatedCode) {
         removeError(rowElement, errorListElement)
         return {system: valueSystem, code: valueValue}
    } else {
        addError(rowElement, errorListElement, errors, id, "System or code not usable for coding")
        return null
    }
}

function addError(rowElement, errorListElement, errors, id, message) {
    errors.push({id: id, error: message})

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
    const requestUrl = fullUrl.slice(0, fullUrl.indexOf("/_history") + 1)
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
        parseResponse(response, false, resourceBaseUrlWithoutId)
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
        parseResponse(response, true, requestUrl)
    })
}

function parseResponse(response, redirect, resourceBaseUrlWithoutId) {
    console.log(response)

    response.text().then((text) => {
        console.log(text)

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

function adaptFormInputs() {
//    const resourceType = getResourceTypeForCurrentUrl();
//
//    if (resourceType !== null && resourceType[1] !== undefined && resourceType[1] === 'Task') {
//        const task = getResourceAsJson()
//
//        if (task.meta !== null && task.meta.profile !== null && task.meta.profile.length > 0) {
//            const profile = task.meta.profile[0].split("|")
//
//            if (profile.length > 0) {
//                let currentUrl = window.location.origin + window.location.pathname
//                let requestUrl = currentUrl.slice(0, currentUrl.indexOf("/Task")) + "/StructureDefinition?url=" + profile[0]
//
//                if (profile.length > 1) {
//                    requestUrl = requestUrl + "&version=" + profile[1]
//                }
//
//                fetch(requestUrl, {
//                    method: "GET",
//                    headers: {
//                        'Accept': 'application/json'
//                    }
//                }).then(response => {
//                    console.log(response)
//
//                    if (response.ok) {
//                         response.json().then((json) => {
//                             // TODO
//                         })
//                    }
//                })
//            }
//        }
//    }
}

function getResourceAsJson() {
    const resource = document.getElementById("json").innerText
    return JSON.parse(resource)
}