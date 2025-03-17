async function openDiagram(bpmnXML) {
	const viewer = new BpmnJS({ container: '#bpmn-canvas' })
	try {
		await viewer.importXML(bpmnXML)
		const canvas = viewer.get('canvas')
		canvas.zoom('fit-viewport')
	} catch (err) {
		console.error('Unable to import BPMN diagram', err)
	}
}

window.addEventListener('DOMContentLoaded', () => {
	const bpmn = document.querySelector('#download-link').getAttribute('href')
	fetch(bpmn).then(r => r.blob()).then(b => b.text()).then(xml => openDiagram(xml))
})