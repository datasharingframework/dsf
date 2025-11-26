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