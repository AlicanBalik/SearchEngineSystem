/// <reference path="/home/alican/workspace/SearchEngine/public/javascripts/jquery-1.10.2.min.js" />
function buttonSubmitClicked(event) {

    if (!document.getElementById("q").value) {
        event.preventDefault();
    }
}
$(document).ready(function () {
    $(".contactLi").click(function () {
        $("body").css("background-color", "rgba(0,0,0,0.6)");
        $(".contact").show();
        $(".frame").show();
        $("#logo").hide();
        $("#q").hide();
        $("#btnSearch").hide();
        $(".content").children.hide();
    });
    $(".close").click(function () {
        $(".contact").hide();
        $(".frame").hide();
        $("body").css("background-color", "");
        $("#logo").show();
        $("#q").show();
        $("#btnSearch").show();
    });
});
$(function () {

    $(document).on('scroll', function () {

        if ($(window).scrollTop() > 100) {
            $('.scroll-top-wrapper').addClass('show');
        } else {
            $('.scroll-top-wrapper').removeClass('show');
        }
    });

    $('.scroll-top-wrapper').on('click', scrollToTop);

    $(".pagNumber").click(function () {
        $(this).addClass("active");
        var x = window.location.hash = $(this).attr("href");
        console.log(x);
    });

    var hash = window.location.hash;
    $(".pagNumber a[href=" + hash + "]").addClass("active");

    $(".goBack").click(function () {
        window.history.back();
    });
});

function scrollToTop() {
    verticalOffset = typeof (verticalOffset) != 'undefined' ? verticalOffset : 0;
    element = $('body');
    offset = element.offset();
    offsetTop = offset.top;
    $('html, body').animate({
        scrollTop: offsetTop
    }, 500, 'linear');
}