$(function () {
    // 7 bind to events triggered on the tree
    $('#js-tree')
        .on("changed.jstree", function (e, data) {
            var i, j, r = [];
            for (i = 0, j = data.selected.length; i < j; i++) {
                r.push(data.instance.get_node(data.selected[i]).text);
            }

            console.log(data.selected);
            console.log('Selected: ' + r.join(', '));
        })
        .jstree({
            "plugins": ["wholerow", "checkbox", "json_data", "search"],
            'core': {
                'data': {
                    'url': function (node) {
                        // console.log(node);
                        // console.log(DeviceUid);

                        // var $divParameters = $("#js-tree");
                        // var par_deviceUid = $divParameters.data('param1');
                        // console.log(par_deviceUid);

                        var deviceUid = (function (deviceUid) {
                            return deviceUid.value;
                        })(DeviceUid);
                        // console.log(deviceUid);

                        // node.id === '#' ?
                        return '/sync-compare-directory/children/' + deviceUid;
                    },
                    'data': function (node) {
                        return {'id': node.id};
                    }
                }
            }
        });
});

$("#search-js-tree").submit(function (e) {
    e.preventDefault();
    $("#js-tree").jstree(true).search($("#qry-js-tree").val());
});
