$(function () {
    function log(message) {
        $("<div>").text(message).prependTo("#log");
        $("#log").scrollTop(0);
    }

    $("#fname").autocomplete({
        source: function (request, response) {
            $.ajax({
                url: "/search-by-file-name",
                data: {name: request.term},
                dataType: "json",
                success: response,
                error: function () {
                    response([]);
                }
            });
        },
        // open: function() {
        //     $('#fname').autocomplete("widget").width(900)
        // },
        minLength: 2,
        select: function (event, ui) {
            $.ajax({
                url: "/get-files-by-file-name",
                data: {name: ui.item.value},
                dataType: "json",
                success: function (result) {
                    // console.log(result);
                    // $("#log").html(result);

                    $.each(result, function (index, value) {
                        console.log(value);
                        log(value.name + " | " + value.path);
                    });

                },
                error: function (request, status, error) {
                    alert(request.responseText);
                }
            });

        }
    });
});
