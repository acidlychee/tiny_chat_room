var stompClient = null;

function setConnected(connected) {
    $("#login").prop("disabled", connected);
    $("#logout").prop("disabled", !connected);
}

function connect() {
    var socket = new SockJS('/app-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/queue/admin', function (greeting) {
            showMessage(JSON.parse(greeting.body).content);
        });
        stompClient.send("/app/room", {}, JSON.stringify({}));
        $("#conversation").show();
        $("#greetings").html("");
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function send() {
    if (stompClient == null) {
       alert("Please login... ")
    }
    var result = stompClient.send("/app/message", {}, JSON.stringify({'content': $("#message").val()}));
    console.log("send result:" + result);
}

function loginRoom() {
    stompClient.subscribe('/user/queue/chat', function (greeting) {
        showMessage(JSON.parse(greeting.body).content);
    }, {'name': $("#name").val(), 'room': $("#room").val()});
    setConnected(true);
}

function logoutRoom() {
    console.log("Logout...");
    disconnect();
}

function showMessage(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#login" ).click(function() { loginRoom(); });
    $( "#logout" ).click(function() { logoutRoom(); });
    $( "#send" ).click(function() { send(); });
    connect();
});