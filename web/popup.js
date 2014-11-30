function popUp(url, name, width, height) {
    var windowHandle = window.open(url, name, 'width=' + width + ',height=' + height);
    windowHandle.close();
    var wh2 = window.open(url, name, 'width=' + width + ',height=' + height);
    wh2.moveTo(window.screenX - 100, window.screenY + 200);
    window.focus();
    return false;
}
