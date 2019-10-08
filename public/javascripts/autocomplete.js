$(function () {
    function log(message) {
        $("<div>").text(message).prependTo("#log");
        $("#log").scrollTop(0);
    }

    var table = $('#fc-table-filter').DataTable({
        "serverSide": true,
        "ajax": "/get-files-by-file-name",

        "columns": [
            // {"data": "id"},
            {
                "data": "name",
                "render": function (data, type, row, meta) {
                    if (type === 'display') {
                        if (row.sha256)
                            data = '<a href="view-file?sha256=' + row.sha256 + '">' + data + '</a>';
                        else data
                    }
                    return data;
                }
            },
            {"data": "path"},
            {"data": "sha256"}
        ]
        // "paging": true
        // "processing": true,
    });

});
