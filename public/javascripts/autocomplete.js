$(function () {
    function log(message) {
        $("<div>").text(message).prependTo("#log");
        $("#log").scrollTop(0);
    }

    var table = $('#fc-table-filter').DataTable({
        "columns": [
            // {"data": "id"},
            {
                "data": "name",
                "render": function(data, type, row, meta){
                    if(type === 'display'){
                        data = '<a href="view-file?sha256=' + row.sha256 + '">' + data + '</a>';
                    }
                    return data;
                }
            },
            {"data": "path"},
            {"data": "sha256"}
        ]
    });

    $("#f-name").autocomplete({
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
        //     $('#f-name').autocomplete("widget").width(900)
        // },
        minLength: 2,
        select: function (event, ui) {
            $.ajax({
                url: "/get-files-by-file-name",
                data: {name: ui.item.value},
                dataType: "json",
                success: function (result) {
                    table.rows.add(result).draw();
                },
                error: function (request, status, error) {
                    alert(request.responseText);
                }
            });

        }
    });
});
