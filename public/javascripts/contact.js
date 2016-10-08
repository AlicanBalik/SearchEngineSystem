function buttonClick(event) {
	
	var nameSurname = document.getElementById("nameSurname");
	var email = document.getElementById("email");
	var message = document.getElementById("message");
	var title = document.getElementById("title");
    var testEmail = /^[A-Z0-9._%+-]+@([A-Z0-9-]+\.)+[A-Z]{2,4}$/i;
    if (nameSurname.value.length == 0 || email.value.length == 0 || message.value.length == 0 || title.value.length == 0 || !testEmail.test(email.value)) {
        event.preventDefault();
        alert("Please check the form!");
    } else {
    	alert("You have successfully sent your mail. We will reply you back as fast as possible.");
    }
}