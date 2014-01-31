function popUp(url, name, width, height) {
    var windowHandle = window.open(url, name, 'width=' + width + ',height=' + height);
    windowHandle.close();
    var wh2 = window.open(url, name, 'width=' + width + ',height=' + height);
    wh2.moveTo(0, 0);
    window.focus();
    return false;
}
