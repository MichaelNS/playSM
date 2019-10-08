$(function () {
    $('#fc-table-filter').DataTable({
        "serverSide": true,
        "ajax": "/get-files-by-file-name",
        "columns": [
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
    });
});
