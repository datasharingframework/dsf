function openTab(lang) {
    const tabcontent = document.getElementsByClassName("prettyprint")
    for (let i = 0; i < tabcontent.length; i++)
        tabcontent[i].style.display = "none"

    const tablinks = document.getElementsByClassName("tablinks")
    for (let i = 0; i < tablinks.length; i++)
        tablinks[i].className = tablinks[i].className.replace(" active", "")

    document.getElementById(lang).style.display = "block"
    document.getElementById(lang + "-button").className += " active"

    if (lang != "html" && localStorage != null)
        localStorage.setItem('lang', lang)
    
    if (lang == "html")
        lang = localStorage != null && localStorage.getItem("lang") != null ? localStorage.getItem("lang") : "xml"

    setDownloadLink(lang)
}

function openInitialTab(htmlEnabled) {
    if (htmlEnabled)
        openTab("html")
    else {
        const lang = localStorage != null && localStorage.getItem("lang") != null ? localStorage.getItem("lang") : "xml"
        if (lang == "xml" || lang == "json")
            openTab(lang);
    }
}

function setDownloadLink(lang) {
    const searchParams = new URLSearchParams(document.location.search)
    searchParams.set('_format', lang)
    searchParams.set('_pretty', 'true')

    const downloadLink = document.getElementById('download-link')
    downloadLink.href = '?' + searchParams.toString()
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