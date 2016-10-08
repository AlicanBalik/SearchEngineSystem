function buttonSubmitClicked(event) {

    if (!document.getElementById("inputFile").value || !document.getElementById("email").value) {
        event.preventDefault();
        alert("Please fill out the form!");
    }
}
// file size ayarla.
function myFunction(){
    var x = document.getElementById("inputFile");
    var txt = "";
    if ('files' in x) {
        if (x.files.length == 0) {
            txt = "Select a PDF document.";
        } else {
            for (var i = 0; i < x.files.length; i++) {
                //txt += "<br><strong>" + (i+1) + ". file</strong><br>";
            	var file = x.files[i];
            	if(file.size > 25000000) {
                	event.preventDefault();
                	document.getElementById("inputFile").value = "";
                	alert("File size must be smaller than 25MB(25000000 Bytes). If your file is more than 25MB, please contact us. ");
                } else {
                	if ('name' in file) {
                        txt += "name: " + file.name + "<br>";
                    }
                    if ('size' in file) {
                        txt += "size: " + file.size + " bytes <br>";
                    }
                } 
            }
        }
    } 
    else {
        if (x.value == "") {
            txt += "Select a PDF document.";
        } else {
            txt += "The files property is not supported by your browser!";
            txt  += "<br>The path of the selected file: " + x.value; // If the browser does not support the files property, it will return the path of the selected file instead. 
        }
    }
    document.getElementById("details").innerHTML = txt;
}
$(function () {
    var dropZoneId = "drop-zone";
    var buttonId = "clickHere";
    var mouseOverClass = "mouse-over";

    var dropZone = $("#" + dropZoneId);
    var ooleft = dropZone.offset().left;
    var ooright = dropZone.outerWidth() + ooleft;
    var ootop = dropZone.offset().top;
    var oobottom = dropZone.outerHeight() + ootop;
    var inputFile = dropZone.find("input");
    document.getElementById(dropZoneId).addEventListener("dragover", function (e) {
        e.preventDefault();
        e.stopPropagation();
        dropZone.addClass(mouseOverClass);
        var x = e.pageX;
        var y = e.pageY;

        if (!(x < ooleft || x > ooright || y < ootop || y > oobottom)) {
            inputFile.offset({ top: y - 15, left: x - 100 });
        } else {
            inputFile.offset({ top: -400, left: -400 });
        }

    }, true);

    if (buttonId != "") {
        var clickZone = $("#" + buttonId);

        var oleft = clickZone.offset().left;
        var oright = clickZone.outerWidth() + oleft;
        var otop = clickZone.offset().top;
        var obottom = clickZone.outerHeight() + otop;

        $("#" + buttonId).mousemove(function (e) {
            var x = e.pageX;
            var y = e.pageY;
            if (!(x < oleft || x > oright || y < otop || y > obottom)) {
                inputFile.offset({ top: y - 15, left: x - 160 });
            } else {
                inputFile.offset({ top: -400, left: -400 });
            }
        });
    }

    document.getElementById(dropZoneId).addEventListener("drop", function (e) {
        $("#" + dropZoneId).removeClass(mouseOverClass);
    }, true);

})