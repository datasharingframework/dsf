function startProcess() {
    const taskStringBefore = document.getElementById("json").innerText
    const task = JSON.parse(taskStringBefore)

    const errors = []
    readTaskInputsFromForm(task, errors)

    console.log(task)
    console.log(errors)

    if (errors.length === 0) {
        const taskStringAfter = JSON.stringify(task)
        createTask(taskStringAfter)
    }
}

function readTaskInputsFromForm(task, errors) {
    task.status = "requested"

    // TODO set requester as practitioner-identifier if OIDC
    //task.requester.type = "Practitioner"
    //task.requester.identifier.value = ""
    //task.requester.identifier.system = "http://dsf.dev/sid/practitioner-identifier"

    task.authoredOn = new Date().toISOString()
    task.meta.lastUpdated = null
    task.meta.version = null

    task.input.forEach((input) => {
        if (input.hasOwnProperty("type")) {
            const id = input.type.coding[0].code

            if (id !== "message-name" && id !== "business-key" && id !== "correlation-key") {
                const inputValueType = Object.keys(input).find((string) => string.startsWith("value"))
                input[inputValueType] = readAndValidateValue(id, inputValueType, errors)
            }
        }
    })
}

function completeQuestionnaireResponse() {
    const questionnaireResponseStringBefore = document.getElementById("json").innerText
    const questionnaireResponse = JSON.parse(questionnaireResponseStringBefore)

    const errors = []
    readQuestionnaireResponseAnswersFromForm(questionnaireResponse, errors)

    console.log(questionnaireResponse)
    console.log(errors)

    if (errors.length === 0) {
        const questionnaireResponseStringAfter = JSON.stringify(questionnaireResponse)
        updateQuestionnaireResponse(questionnaireResponseStringAfter)
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

                answer[answerType] = readAndValidateValue(id, answerType, errors)
            }
        }
    })
}

function readAndValidateValue(id, valueType, errors) {
    const value = document.getElementById(id).value

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
    } else if (valueType === 'valueUri') {
        return validateUrl(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueReference') {
        return validateReference(rowElement, errorListElement, value, errors, id)
    } else if (valueType === 'valueBoolean') {
        return document.querySelector("input[name=" + id + "]:checked").value
    } else if (valueType === "valueIdentifier") {
        // TODO
        return null
    } else if (valueType === "valueCoding") {
        // TODO
        return null
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

    const status = response.status
    const statusOk = response.ok
    const statusText = response.statusText === null ? " - " + response.statusText : ""

    response.text().then((text) => {
        console.log(text)

        if (statusOk) {
            const resource = JSON.parse(text)
            setTimeout(() => {
                disableSpinner()
                window.location.href = resourceBaseUrlWithoutId + "/" + resource.id
            }, 1000)
        } else {
            disableSpinner()
            const alertText = "Status: " + status + statusText + "\n\n" + text
            window.alert(alertText)
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
    // TODO set requester as practitioner-identifier if OIDC
    // TODO load cardinalities and add inputs
    console.log("Cardinalities to be loaded..")
}