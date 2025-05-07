<?php
// --- 配置区域 ---
define('TRUENAS_API', 'http://192.168.9.141/api/v2.0/');
define('TRUENAS_TOKEN', '1-WvOjR7HFrYFDkd4O29x32O3C7c88lPo9YvRScYhlrOCGPcVRXecToN47hnOec3XZ'); // 替换为你的实际 token

define('SNAPSHOT', 'win10/4060-base@init');
define('GAME_SNAPSHOT','game/game@wooduan');
define('PORTAL_ID', 1);  // TrueNAS 中默认 portal ID（通常是 2）
define('DB_DSN', 'mysql:host=localhost;dbname=fog;charset=utf8');
define('DB_USER', 'nasweb'); // <<< 替换
define('DB_PASS', 'nasweb'); // <<< 替换
// define('ZVOL_PATH', 'vhd-store');

// --- 获取 MAC ---
if (!isset($_GET['mac'])) {
    http_response_code(400);
    echo "Missing MAC";
    exit;
}
$mac = strtolower(str_replace('-', ':', $_GET['mac']));


$clientName = 'client-' . str_replace(':', '', $mac);
// 数据集
$dataset = "win10/clone/{$clientName}";
$vhdPath = "/mnt/{$dataset}/win10-dev.vhd";
$gameClientName = 'game-' . str_replace(':', '', $mac);
// 数据集
$gameDataset = "game/clone/{$gameClientName}";
$gameVhdPath = "zvol/game/clone/game-wooduan";



// 数据库查找
try {
    $pdo = new PDO(DB_DSN, DB_USER,DB_PASS);
} catch (PDOException $e) {
    http_response_code(500);
    echo "#DB ERROR: " . $e->getMessage();
    exit;
}

// --- 查询是否已注册 ---
$stmt = $pdo->prepare('SELECT name FROM mac_table WHERE mac = ?');
$stmt->execute([$mac]);
$entry = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$entry) {
    // --- 保存数据库 ---
    $stmt = $pdo->prepare('INSERT INTO mac_table (mac, name) VALUES (?, ?)');
    $stmt->execute([$mac, $clientName]);
}

// 删除之前iSCSI

function truenas_request($method, $endpoint, $data = null) {
    $ch = curl_init(TRUENAS_API . $endpoint);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, strtoupper($method));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . TRUENAS_TOKEN,
    ]);
    if ($data) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    }
    $response = curl_exec($ch);
    if (curl_errno($ch)) {
    }
    curl_close($ch);
    return json_decode($response, true);
}

// --- 鏌ユ壘骞跺垹闄?targetextent ---
$bindings = truenas_request("GET", "iscsi/targetextent?target=$clientName");
foreach ($bindings as $bind) {
    truenas_request("DELETE", "iscsi/targetextent/id/{$bind['id']}");
}

// --- 鏌ユ壘骞跺垹闄?extent ---
$extents = truenas_request("GET", "iscsi/extent?name=$clientName");
foreach ($extents as $ext) {
    truenas_request("DELETE", "iscsi/extent/id/{$ext['id']}");
}

// --- 鏌ユ壘骞跺垹闄?target ---
$targets = truenas_request("GET", "iscsi/target?name=$clientName");
foreach ($targets as $t) {
    truenas_request("DELETE", "iscsi/target/id/{$t['id']}");
}

// // ======= 鍒犻櫎鍏嬮殕鍑虹殑 ZFS dataset锛坴hd-store/client-xxxxxx锛?=======
// $datasetName = ZVOL_PATH . '/' . $clientName;

// 娉ㄦ剰 URL 缂栫爜璺緞涓殑 "/"
$encodedDataset = rawurlencode($dataset);

// DELETE /zfs/dataset/<encoded name>?recursive=true
$response = truenas_request("DELETE", "pool/dataset/id/$encodedDataset?recursive=true");



    // --- 创建克隆数据集 ---
    truenas_post('zfs/snapshot/clone', [
        'snapshot' => SNAPSHOT,
        'dataset_dst' => $dataset
    ]);

    // ---  ^h^{    target ---
    truenas_post('iscsi/target', [
        'name' => $clientName,
        'mode' => 'ISCSI',
        'groups' => [[
            'portal' => PORTAL_ID,
            'initiator' => null,
            'authmethod' => 'NONE',
            'auth' => null
        ]]
    ]);

    // --- 创建 extent ---
    truenas_post('iscsi/extent', [
        "name" => $clientName,
        "enabled" => true,
        "type" => "FILE",
        "path" => $vhdPath,
        "filesize" => 0,
	"blocksize" => 512,
        "insecure_tpc" => true,
        "rpm" => "SSD"
    ]);
    
 //  echo " iscsi/extent :$vhdPath";

    // 等待资源刷新（可选）

    // 获取 target 和 extent 的 ID
    $targetId = get_id_by_name('iscsi/target', $clientName);
    $extentId = get_id_by_name('iscsi/extent', $clientName);

//    echo "boot php targetId:$targetId\n" ;
//    echo "boot php extentId:$extentId\n" ;

    if (!$targetId || !$extentId) {
        http_response_code(500);
        echo "获取 ID 失败";
        exit;
    }


    truenas_post('iscsi/targetextent', [
        "target" => $targetId,
        "extent" => $extentId,
        "lunid" => 1
    ]);



 // 创建游戏盘


// 删除之前iSCSI
// --- 鏌ユ壘骞跺垹闄?targetextent ---
$bindings = truenas_request("GET", "iscsi/targetextent?target=$gameClientName");
foreach ($bindings as $bind) {
    truenas_request("DELETE", "iscsi/targetextent/id/{$bind['id']}");
}

// --- 鏌ユ壘骞跺垹闄?extent ---
$extents = truenas_request("GET", "iscsi/extent?name=$gameClientName");
foreach ($extents as $ext) {
    truenas_request("DELETE", "iscsi/extent/id/{$ext['id']}");
}

// --- 鏌ユ壘骞跺垹闄?target ---
$targets = truenas_request("GET", "iscsi/target?name=$gameClientName");
foreach ($targets as $t) {
    truenas_request("DELETE", "iscsi/target/id/{$t['id']}");
}

// 娉ㄦ剰 URL 缂栫爜璺緞涓殑 "/"
$encodedDataset = rawurlencode($gameDataset);

// DELETE /zfs/dataset/<encoded name>?recursive=true
$response = truenas_request("DELETE", "pool/dataset/id/$encodedDataset?recursive=true");



    // --- 创建克隆数据集 ---
    truenas_post('zfs/snapshot/clone', [
        'snapshot' => GAME_SNAPSHOT,
        'dataset_dst' => $gameDataset
    ]);

    // ---  ^h^{    target ---
    truenas_post('iscsi/target', [
        'name' => $gameClientName,
        'mode' => 'ISCSI',
        'groups' => [[
            'portal' => PORTAL_ID,
            'initiator' => null,
            'authmethod' => 'NONE',
            'auth' => null
        ]]
    ]);

    // --- 创建 extent ---
    truenas_post('iscsi/extent', [
        "name" => $gameClientName,
        "enabled" => true,
        "type" => "DISK",
        "disk" => $gameVhdPath,
	    "blocksize" => 512,
        "insecure_tpc" => true,
        "rpm" => "SSD"
    ]);

 //  echo " iscsi/extent :$vhdPath";

    // 等待资源刷新（可选）

    // 获取 target 和 extent 的 ID
    $gameTargetId = get_id_by_name('iscsi/target', $gameClientName);
    $gameExtentId = get_id_by_name('iscsi/extent', $gameClientName);

//    echo "boot php targetId:$targetId\n" ;
//    echo "boot php extentId:$extentId\n" ;

    if (!$gameTargetId || !$gameExtentId) {
        http_response_code(500);
        echo "获取 game ID 失败";
        exit;
    }


    truenas_post('iscsi/targetextent', [
        "target" => $gameTargetId,
        "extent" => $gameExtentId,
        "lunid" => 1
    ]);


// --- 返回 iPXE 启动信息 ---
header('Content-Type: text/plain');
$iqn = "iqn.2005-10.zlb:$clientName"; // 自定义 IQN 前缀
echo "#!ipxe\n";
echo "sanboot --keep iscsi:192.168.9.141::3260:1:$iqn\n";

// --- TrueNAS API 调用函数 ---
function truenas_post($endpoint, $data) {
    $ch = curl_init(TRUENAS_API . $endpoint);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . TRUENAS_TOKEN,
            'Content-Type: application/json'
        ],
        CURLOPT_POSTFIELDS => json_encode($data)
    ]);
    $res = curl_exec($ch);
    $http = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    if ($http >= 300) {
        http_response_code(500);
        echo "API Error [$endpoint]: $res";
        exit;
    }
    curl_close($ch);
}

function truenas_get($endpoint) {
    $ch = curl_init(TRUENAS_API . $endpoint);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . TRUENAS_TOKEN
        ]
    ]);
    $res = curl_exec($ch);
    curl_close($ch);
    return json_decode($res, true);
}

function get_id_by_name($endpoint, $name) {
    $list = truenas_get($endpoint);
    foreach ($list as $item) {
        if ($item['name'] === $name) {
            return $item['id'];
        }
    }
    return null;
}
