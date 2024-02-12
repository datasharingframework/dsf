function openTab(lang) {
	if ((('html' !== lang) || ('html' === lang && history.state?.lang !== undefined)) && history.state?.lang !== lang)
		history.pushState({ lang: lang }, '', window.location.href)
	
	setDownloadLink(lang === 'json' ? 'json' : 'xml')
	
	if('html' === lang && document.querySelector('div#html') === null)
		lang = 'xml'
	
    const tabcontent = document.getElementsByClassName("prettyprint")
    for (let i = 0; i < tabcontent.length; i++)
        tabcontent[i].style.display = "none"

    const tablinks = document.getElementsByClassName("tablinks")
    for (let i = 0; i < tablinks.length; i++)
        tablinks[i].className = tablinks[i].className.replace(" active", "")

    document.getElementById(lang).style.display = "block"
    document.getElementById(lang + "-button").className += " active"
}

function openInitialTab() {
	if (history.state?.lang === 'html' || history.state?.lang === 'json' || history.state?.lang === 'xml')
		openTab(history.state?.lang)		
	else
		openTab('html')		
}

function setDownloadLink(lang) {
    const searchParams = new URLSearchParams(document.location.search)
    searchParams.set('_format', lang)
    searchParams.set('_pretty', 'true')

    const downloadLink = document.getElementById('download-link')
    downloadLink.href = window.location.origin + window.location.pathname + '?' + searchParams.toString()
    downloadLink.download = getDownloadFileName(lang)
    downloadLink.title = 'Download as ' + lang.toUpperCase()
}

function getDownloadFileName(lang) {
    const resourceType = getResourceTypeForCurrentUrl()

    /* /, /metadata, /_history */
    if (resourceType == null) {
        if (window.location.pathname.endsWith('/metadata'))
            return "metadata." + lang
        else if (window.location.pathname.endsWith('/_history'))
            return "history." + lang
        else
            return "root." + lang
    } else {
        //Resource
        if (resourceType[1] !== undefined && resourceType[2] === undefined && resourceType[3] === undefined && resourceType[4] === undefined)
            return resourceType[1] + '_Search.' + lang
        //Resource/_history
        else if (resourceType[1] !== undefined && resourceType[2] === undefined && resourceType[3] !== undefined && resourceType[4] === undefined)
            return resourceType[1] + '_History.' + lang
        //Resource/id
        else if (resourceType[1] !== undefined && resourceType[2] !== undefined && resourceType[3] === undefined && resourceType[4] === undefined)
            return resourceType[1] + '_' + resourceType[2].replace('/', '') + '.' + lang
        //Resource/id/_history
        else if (resourceType[1] !== undefined && resourceType[2] !== undefined && resourceType[3] !== undefined && resourceType[4] === undefined)
            return resourceType[1] + '_' + resourceType[2].replace('/', '') + '_History.' + lang
        //Resource/id/_history/version
        else if (resourceType[1] !== undefined && resourceType[2] !== undefined && resourceType[3] !== undefined && resourceType[4] !== undefined)
            return resourceType[1] + '_' + resourceType[2].replace('/', '') + '_v' + resourceType[4].replace('/', '') + '.' + lang
    }
}