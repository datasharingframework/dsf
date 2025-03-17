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
	
	document.querySelector('div#icons > svg#light-mode')?.addEventListener('click', () => setUiMode('light'))
	document.querySelector('div#icons > svg#dark-mode')?.addEventListener('click', () => setUiMode('dark'))
})