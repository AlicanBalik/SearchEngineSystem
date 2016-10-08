function buttonClick(event) {
	var nameSurname = document.getElementById("nameSurname");
	var email = document.getElementById("email");
	var message = document.getElementById("message");
	var title = document.getElementById("title");
    if (nameSurname.value.length == 0|| email.value.length == 0 || message.value.length == 0 || title.value.length == 0) {
        event.preventDefault();
        alert("Please fill out the form!");
    } else {
    	alert("You have successfully sent your mail. We will reply you back as fast as possible.")
    }
}