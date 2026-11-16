package io.github.xiaolong125.lethe;

interface IUninstallService {
    int getUid() = 0;
    String uninstall(String packageName) = 1;
    String restore(String packageName) = 2;
    // Shizuku reserves this transaction code for UserService teardown.
    void destroy() = 16777114;
}
