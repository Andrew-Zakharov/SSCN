#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QtNetwork>
#include <QDebug>

namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(const QString& hostName, quint16 port, QWidget *parent = 0);
    ~MainWindow();

private slots:
    void OnSendClicked();
    void socketReadyRead();
    void socketConnected();
    void socketError(QAbstractSocket::SocketError socketError);

private:
    Ui::MainWindow *ui;
    QTcpSocket* tcpSocket;
    QDataStream dataStream;
};

#endif // MAINWINDOW_H
