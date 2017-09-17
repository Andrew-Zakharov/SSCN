#include "mainwindow.h"
#include <QApplication>

#include <server.h>

int main(int argc, char *argv[])
{
    QApplication application(argc, argv);
    QGuiApplication::setApplicationDisplayName(Server::tr("Server"));
    Server server;
    server.show();

    return application.exec();
}
