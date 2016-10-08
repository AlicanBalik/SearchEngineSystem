function confirm() {
    if (confirm("Press OK if you sent contributer a message.") != true) {
    	event.preventDefault();
    }
}