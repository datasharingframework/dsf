function setUiTheme(theme = getUiTheme()) {
    if (theme === 'dark') {
        document.getElementById('light-mode').style.display = 'block'
        document.getElementById('dark-mode').style.display = 'none'
    }
    else {
        document.getElementById('light-mode').style.display = 'none'
        document.getElementById('dark-mode').style.display = 'block'
    }
    
    document.querySelector("html").setAttribute("theme", theme);
    localStorage.setItem("theme", theme);
}

function getUiTheme() {
    if (localStorage !== null && localStorage.getItem("theme") !== null)
        return localStorage.getItem("theme")
    else if (window.matchMedia("(prefers-color-scheme: dark)").matches)
    	return "dark"
    else
        return "light"
}

window.addEventListener('DOMContentLoaded', () => {
	setUiTheme()
	prettyPrint()
	checkBookmarked()
	openInitialTab()

	const resourceType = getResourceTypeForCurrentUrl()
	adaptTaskFormInputs(resourceType)
	adaptQuestionnaireResponseInputsIfNotVersion1_0_0(resourceType)

	document.querySelector('div#icons > svg#help-icon')?.addEventListener('click', () => showHelp())
	document.querySelector('div#icons > svg#light-mode')?.addEventListener('click', () => setUiTheme('light'))
	document.querySelector('div#icons > svg#dark-mode')?.addEventListener('click', () => setUiTheme('dark'))
	document.querySelector('div#icons > svg#bookmark-add')?.addEventListener('click', () => addCurrentBookmark())
	document.querySelector('div#icons > svg#bookmark-remove')?.addEventListener('click', () => removeCurrentBookmark())
	document.querySelector('div#icons > svg#bookmark-list')?.addEventListener('click', () => showBookmarks())

	document.querySelector('div#help > svg#help-close')?.addEventListener('click', () => closeHelp())
	document.querySelector('div#bookmarks > svg#bookmark-list-close')?.addEventListener('click', () => closeBookmarks())

	document.querySelector('div.tab > button#html-button')?.addEventListener('click', () => openTab('html'))
	document.querySelector('div.tab > button#json-button')?.addEventListener('click', () => openTab('json'))
	document.querySelector('div.tab > button#xml-button')?.addEventListener('click', () => openTab('xml'))

	const resourceType = getResourceTypeForCurrentUrl();

	if (resourceType != null && resourceType[1] != null && resourceType[2] === undefined && resourceType[3] === undefined && resourceType[4] === undefined) {

		// search bundle rows
		document.querySelectorAll('div#html > div.bundle > div#list td.id-value:first-child').forEach(td => {
			if (td?.firstChild?.href) {
				td.parentElement.addEventListener('click', event => {
					if (event.target?.tagName?.toLowerCase() !== 'a')
						window.location = td.firstChild.href
				})
			}
		})
	}


	if (resourceType != null && resourceType[1] === 'QuestionnaireResponse' && resourceType[2] !== null) {

		// input placeholder insert buttons
		document.querySelectorAll('form > fieldset#form-fieldset > div.row input').forEach(input => {
			if (input?.nextElementSibling?.tagName?.toLowerCase() === 'svg') {
				input.nextElementSibling.addEventListener('click', () => {
					if (input?.placeholder !== '') {
						input.text = input.placeholder
						input.value = input.placeholder
					}
				})
			}
		})

		// complete questionnaire response
		document.querySelector('form > fieldset#form-fieldset > div.row-submit > button#complete-questionnaire-response')?.addEventListener('click', () => completeQuestionnaireResponse())
	}

	if (resourceType != null && resourceType[1] === 'Task' && resourceType[2] !== null) {

		// input placeholder insert buttons
		document.querySelectorAll('form > fieldset#form-fieldset > section#inputs > div.row input').forEach(input => {
			if (input?.nextElementSibling?.tagName?.toLowerCase() === 'svg') {
				input.nextElementSibling.addEventListener('click', () => {
					if (input?.placeholder !== '') {
						input.text = input.placeholder
						input.value = input.placeholder
					}
				})
			}
		})

		// start process button
		document.querySelector('form > fieldset#form-fieldset > div.row-submit > button#start-process')?.addEventListener('click', () => startProcess())
	}
})