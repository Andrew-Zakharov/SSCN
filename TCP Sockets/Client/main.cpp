#include "mainwindow.h"
#include <QApplication>

#include "connectionwindow.h"

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    ConnectionWindow w;
    w.show();

    return a.exec();
}
