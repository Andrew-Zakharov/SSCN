#ifndef CONNECTIONWINDOW_H
#define CONNECTIONWINDOW_H

#include <QMainWindow>
#include "mainwindow.h"
#include <QDebug>

namespace Ui {
class ConnectionWindow;
}

class ConnectionWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit ConnectionWindow(QWidget *parent = 0);
    ~ConnectionWindow();

private slots:
    void onConnectClicked();
    void onServerSettingsChanged();

private:
    Ui::ConnectionWindow *ui;
    MainWindow* mainWindow;
};

#endif // CONNECTIONWINDOW_H
