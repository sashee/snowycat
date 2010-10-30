function findAbsolutePosition(obj) {
	var curleft = curtop = 0;
        var width=obj.offsetWidth;
        var height=obj.offsetHeight;
	if (obj.offsetParent) {
		do {
			curleft += obj.offsetLeft;
			curtop += obj.offsetTop;
		} while (obj = obj.offsetParent);
	}
	return [curleft,curtop,width,height];
}

function getElementsAtPosition(X,Y) {
    var vissza=new Array();
    var a=window.document.getElementsByTagName("*");
    for(var i=0;i<a.length;i++){
        var pos=findAbsolutePosition(a[i]);
        var posX=pos[0];
        var posY=pos[1];
        var width=pos[2];
        var height=pos[3];
        if(X>=posX && Y>=posY && X<posX+width && Y<posY+height) vissza[vissza.length]=a[i];
    }
    return vissza;
}

function selectByZIndex(elements) {
    var maxZIndex=-999;
    for(var i=0;i<elements.length;i++){
        if(elements[i].style.zIndex>maxZIndex){
            maxZIndex=elements[i].style.zIndex;
        }
    }
    for(var i=0;i<elements.length;i++){
        if(elements[i].style.zIndex<maxZIndex){
            elements.splice(i,1);
            i--;
        }
    }
    return elements;
}

function selectClosest(elements){
    var maxY=-1;
    var closestElement=null;
    for(var i=0;i<elements.length;i++){
	var Y=findAbsolutePosition(elements[i])[1];
	if(Y>maxY){
            maxY=Y;
            closestElement=elements[i];
        }
    }
    return closestElement;
}

function getElementXPath(elt)
{
     var path = "";
     for (; elt && elt.nodeType == 1; elt = elt.parentNode)
     {
   	idx = getElementIdx(elt);
	xname = elt.localName;
	if (idx > 1) xname += "[" + idx + "]";
	path = "/" + xname + path;
     }
 
     return path;	
}

function getElementIdx(elt)
{
    var count = 1;
    for (var sib = elt.previousSibling; sib ; sib = sib.previousSibling)
    {
        if(sib.nodeType == 1 && sib.tagName == elt.tagName)	count++
    }
    
    return count;
}
