function setUiMode(mode = getUiMode()) {
	if (mode === 'dark') {
		document.getElementById('light-mode').style.display = 'block'
		document.getElementById('dark-mode').style.display = 'none'
	}
	else {
		document.getElementById('light-mode').style.display = 'none'
		document.getElementById('dark-mode').style.display = 'block'
	}

	document.querySelector("html").setAttribute("mode", mode)
	localStorage.setItem("mode", mode)
}

function getUiMode() {
	if (localStorage !== null && localStorage.getItem("mode") !== null)
		return localStorage.getItem("mode")
	else if (window.matchMedia("(prefers-color-scheme: dark)").matches)
		return "dark"
	else
		return "light"
}

window.addEventListener('DOMContentLoaded', () => {
	setUiMode()
	prettyPrint()
	checkBookmarked()
	openInitialTab()

	document.querySelector('div#icons > svg#help-icon')?.addEventListener('click', () => showHelp())
	document.querySelector('div#icons > svg#light-mode')?.addEventListener('click', () => setUiMode('light'))
	document.querySelector('div#icons > svg#dark-mode')?.addEventListener('click', () => setUiMode('dark'))
	document.querySelector('div#icons > svg#bookmark-add')?.addEventListener('click', () => addCurrentBookmark())
	document.querySelector('div#icons > svg#bookmark-remove')?.addEventListener('click', () => removeCurrentBookmark())
	document.querySelector('div#icons > svg#bookmark-list')?.addEventListener('click', () => showBookmarks())

	document.querySelector('div#help > svg#help-close')?.addEventListener('click', () => closeHelp())
	document.querySelector('div#bookmarks > svg#bookmark-list-close')?.addEventListener('click', () => closeBookmarks())

	document.querySelector('div.tab > button#html-button')?.addEventListener('click', () => openTab('html'))
	document.querySelector('div.tab > button#json-button')?.addEventListener('click', () => openTab('json'))
	document.querySelector('div.tab > button#xml-button')?.addEventListener('click', () => openTab('xml'))

	const resourceType = getResourceTypeForCurrentUrl()
	if (resourceType != null && resourceType[1] && resourceType[2] === undefined && resourceType[3] === undefined && resourceType[4] === undefined) {

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

	if (resourceType != null && resourceType[1] === 'QuestionnaireResponse' && resourceType[2] && (resourceType[3] === undefined || resourceType[4])) {

		adaptQuestionnaireResponseInputsIfNotVersion1_0_0()

		// input placeholder insert buttons
		document.querySelectorAll('form > fieldset#form-fieldset > div.row svg.insert:not([disabled])').forEach(svg => {
			const inputs = svg.parentElement.querySelectorAll("input")
			svg.addEventListener('click', () => {
				inputs.forEach(input => {
					if (input?.placeholder !== '') {
						if (input.type === 'radio')
							input.checked = input.placeholder === 'true'
						else
							input.value = input.placeholder
					}
				})
			})
		})

		// input value copy buttons
		document.querySelectorAll('form > fieldset#form-fieldset > div.row svg.copy:not([disabled])').forEach(svg => {
			const input = svg.parentElement.querySelector("input")
			svg.addEventListener('click', () => {
				if (input.type === 'radio')
					navigator?.clipboard?.writeText(input.checked)
				else
					navigator?.clipboard?.writeText(input.value)
			})
		})

		// complete questionnaire response
		document.querySelector('form')?.addEventListener('submit', event => {
			completeQuestionnaireResponse()
			event.preventDefault()
		})
	}

	if (resourceType != null && resourceType[1] === 'Task' && resourceType[2] && (resourceType[3] === undefined || resourceType[4])) {
		adaptTaskFormInputs()

		// input placeholder insert buttons
		document.querySelectorAll('form > fieldset#form-fieldset > div.row svg.insert:not([disabled])').forEach(svg => {
			const inputs = svg.parentElement.querySelectorAll("input")
			svg.addEventListener('click', () => {
				inputs.forEach(input => {
					if (input?.placeholder !== '') {
						if (input.type === 'radio')
							input.checked = input.placeholder === 'true'
						else
							input.value = input.placeholder
					}
				})
			})
		})

		// input / output value copy buttons
		document.querySelectorAll('form > fieldset#form-fieldset > div.row svg.copy:not([disabled])').forEach(svg => {
			const input = svg.parentElement.querySelector("input")
			svg.addEventListener('click', () => {
				if (input.type === 'radio')
					navigator?.clipboard?.writeText(input.checked)
				else
					navigator?.clipboard?.writeText(input.value)
			})
		})

		// start process button
		document.querySelector('form')?.addEventListener('submit', event => {
			startProcess()
			event.preventDefault()
		})
	}

	document.querySelectorAll(".collapse-button").forEach(button => {
		button.addEventListener("click", () => {
			button.classList.toggle("collapse-button-rotated")

			const parent = button.closest(".collapsable");
			parent.classList.toggle("collapsed");
			parent.classList.toggle("expanded");
		})
	});

	document.querySelectorAll(".collapsable").forEach(element => {
		content = element.querySelector(".content-pre");

		function checkOverflow() {
			if (content.scrollHeight > element.clientHeight) {
				element.classList.add("overflow");
			} else {
				element.classList.add("no-overflow");
			}
		}

		checkOverflow();
	});
})

window.addEventListener("popstate", (event) => {
	openTab(event.state?.lang !== undefined ? event.state?.lang : 'html')
})